package com.nalitech.modules.account.service;

import com.nalitech.modules.account.dto.PlanoModeloDtos.AplicarModeloResponse;
import com.nalitech.modules.account.dto.PlanoModeloDtos.ContaRequest;
import com.nalitech.modules.account.dto.PlanoModeloDtos.ContaResponse;
import com.nalitech.modules.account.dto.PlanoModeloDtos.CreatePlanoModeloRequest;
import com.nalitech.modules.account.dto.PlanoModeloDtos.PlanoModeloResponse;
import com.nalitech.modules.account.entity.ChartAccountKind;
import com.nalitech.modules.account.entity.ChartOfAccount;
import com.nalitech.modules.account.entity.PlanoModelo;
import com.nalitech.modules.account.entity.PlanoModeloConta;
import com.nalitech.modules.account.repository.ChartOfAccountRepository;
import com.nalitech.modules.account.repository.PlanoModeloContaRepository;
import com.nalitech.modules.account.repository.PlanoModeloRepository;
import com.nalitech.modules.client.repository.ClientRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.BusinessException;
import com.nalitech.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlanoModeloService {

    private final PlanoModeloRepository modeloRepository;
    private final PlanoModeloContaRepository contaRepository;
    private final ChartOfAccountRepository chartRepository;
    private final ClientRepository clientRepository;

    public PlanoModeloService(PlanoModeloRepository modeloRepository,
                              PlanoModeloContaRepository contaRepository,
                              ChartOfAccountRepository chartRepository,
                              ClientRepository clientRepository) {
        this.modeloRepository = modeloRepository;
        this.contaRepository = contaRepository;
        this.chartRepository = chartRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<PlanoModeloResponse> list() {
        return modeloRepository.findByEmpresaIdOrderByNomeAsc(SecurityUtils.currentEmpresaId())
                .stream().map(m -> toResponse(m, List.of())).toList();
    }

    @Transactional(readOnly = true)
    public PlanoModeloResponse getById(UUID id) {
        PlanoModelo modelo = find(id);
        return toResponse(modelo, contas(modelo.getId()));
    }

    public PlanoModeloResponse create(CreatePlanoModeloRequest request) {
        PlanoModelo modelo = new PlanoModelo();
        modelo.setEmpresaId(SecurityUtils.requireEmpresaId());
        modelo.setNome(request.nome());
        modelo.setDescricao(request.descricao());
        modelo = modeloRepository.save(modelo);
        return toResponse(modelo, List.of());
    }

    public PlanoModeloResponse addConta(UUID modeloId, ContaRequest request) {
        PlanoModelo modelo = find(modeloId);
        PlanoModeloConta conta = new PlanoModeloConta();
        conta.setEmpresaId(modelo.getEmpresaId());
        conta.setModeloId(modelo.getId());
        conta.setCodigo(request.codigo());
        conta.setNome(request.nome());
        conta.setTipo(request.tipo());
        conta.setAnalitica(ChartAccountKind.normalize(request.tipo()).analitica());
        contaRepository.save(conta);
        return toResponse(modelo, contas(modelo.getId()));
    }

    public void delete(UUID id) {
        PlanoModelo modelo = find(id);
        contas(modelo.getId()).forEach(c -> contaRepository.deleteById(c.id()));
        modeloRepository.delete(modelo);
    }

    /**
     * Copia as contas do modelo para o plano de contas do cliente (spec secao 3).
     * Codigos ja existentes para o cliente sao ignorados (idempotente).
     */
    public AplicarModeloResponse aplicar(UUID modeloId, UUID clienteId) {
        PlanoModelo modelo = find(modeloId);
        UUID empresaId = modelo.getEmpresaId();
        clientRepository.findByIdAndEmpresaId(clienteId, empresaId)
                .orElseThrow(() -> new BusinessException(
                        "Cliente invalido para esta empresa.", HttpStatus.BAD_REQUEST));

        int criadas = 0;
        int ignoradas = 0;
        for (PlanoModeloConta conta : contaRepository
                .findByModeloIdAndEmpresaIdOrderByCodigoAsc(modelo.getId(), empresaId)) {
            if (chartRepository.existsByEmpresaIdAndClienteIdAndCodigo(
                    empresaId, clienteId, conta.getCodigo())) {
                ignoradas++;
                continue;
            }
            ChartOfAccount account = new ChartOfAccount();
            account.setEmpresaId(empresaId);
            account.setClienteId(clienteId);
            account.setCodigo(conta.getCodigo());
            // Modelos usam um unico codigo: classificacao/original espelham o codigo.
            account.setCodigoClassificacao(conta.getCodigo());
            account.setCodigoOriginal(conta.getCodigo());
            account.setNome(conta.getNome());
            account.setTipo(conta.getTipo());
            account.setAnalitica(ChartAccountKind.resolveAnalitica(conta.getAnalitica(), conta.getTipo()));
            chartRepository.save(account);
            criadas++;
        }
        return new AplicarModeloResponse(criadas, ignoradas);
    }

    private List<ContaResponse> contas(UUID modeloId) {
        return contaRepository
                .findByModeloIdAndEmpresaIdOrderByCodigoAsc(modeloId, SecurityUtils.currentEmpresaId())
                .stream()
                .map(c -> new ContaResponse(c.getId(), c.getCodigo(), c.getNome(), c.getTipo()))
                .toList();
    }

    private PlanoModelo find(UUID id) {
        return modeloRepository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Plano-modelo nao encontrado."));
    }

    private PlanoModeloResponse toResponse(PlanoModelo m, List<ContaResponse> contas) {
        return new PlanoModeloResponse(m.getId(), m.getNome(), m.getDescricao(), contas);
    }
}
