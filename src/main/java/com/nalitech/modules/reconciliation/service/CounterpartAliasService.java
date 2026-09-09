package com.nalitech.modules.reconciliation.service;

import com.nalitech.modules.reconciliation.entity.CounterpartAlias;
import com.nalitech.modules.reconciliation.repository.CounterpartAliasRepository;
import com.nalitech.shared.util.DescriptionNormalizer;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aprende e consulta apelidos de contraparte (vinculos confirmados manualmente ou nao):
 * liga o nome do extrato ao nome do sistema para que o match automatico futuro reconheca
 * a mesma parte mesmo com nomes escritos de forma diferente.
 */
@Service
@Transactional
public class CounterpartAliasService {

    private final CounterpartAliasRepository repository;

    public CounterpartAliasService(CounterpartAliasRepository repository) {
        this.repository = repository;
    }

    /** Registra que os dois lados de um match sao a mesma contraparte. */
    public void record(UUID empresaId, UUID clienteId, String descricaoExtrato, String descricaoSistema) {
        String na = DescriptionNormalizer.normalize(descricaoExtrato);
        String nb = DescriptionNormalizer.normalize(descricaoSistema);
        if (na.isBlank() || nb.isBlank() || na.equals(nb)) {
            return; // sem informacao util (nomes vazios ou ja iguais)
        }
        String[] ordenado = canonico(na, nb);
        CounterpartAlias alias = repository
                .findScoped(empresaId, clienteId, ordenado[0], ordenado[1])
                .orElseGet(() -> {
                    CounterpartAlias novo = new CounterpartAlias();
                    novo.setEmpresaId(empresaId);
                    novo.setClienteId(clienteId);
                    novo.setNomeA(ordenado[0]);
                    novo.setNomeB(ordenado[1]);
                    novo.setOcorrencias(0);
                    return novo;
                });
        alias.setOcorrencias(alias.getOcorrencias() + 1);
        repository.save(alias);
    }

    /** True se os dois nomes (ja normalizados) foram aprendidos como a mesma contraparte. */
    @Transactional(readOnly = true)
    public boolean isAlias(UUID empresaId, UUID clienteId, String normalizadoA, String normalizadoB) {
        if (normalizadoA == null || normalizadoB == null
                || normalizadoA.isBlank() || normalizadoB.isBlank() || normalizadoA.equals(normalizadoB)) {
            return false;
        }
        String[] ordenado = canonico(normalizadoA, normalizadoB);
        return repository.findScoped(empresaId, clienteId, ordenado[0], ordenado[1]).isPresent();
    }

    private String[] canonico(String a, String b) {
        return a.compareTo(b) <= 0 ? new String[] {a, b} : new String[] {b, a};
    }
}
