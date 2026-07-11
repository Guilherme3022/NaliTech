package com.nalitech.modules.fiscal.service;

import com.nalitech.modules.fiscal.entity.FiscalObligation;
import com.nalitech.modules.fiscal.entity.ObligationStatus;
import com.nalitech.modules.fiscal.event.ObrigacaoVencendoEvent;
import com.nalitech.modules.fiscal.repository.FiscalObligationRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class FiscalReminderJob {

    private static final int JANELA_DIAS = 3;

    private final FiscalObligationRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public FiscalReminderJob(FiscalObligationRepository repository,
                             ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(cron = "${FISCAL_REMINDER_CRON:0 0 8 * * *}")
    @Transactional(readOnly = true)
    public void enviarLembretes() {
        LocalDate hoje = LocalDate.now();
        List<FiscalObligation> proximas = repository.findByStatusAndVencimentoBetween(
                ObligationStatus.PENDENTE, hoje, hoje.plusDays(JANELA_DIAS));
        log.info("Lembretes fiscais: {} obrigacao(oes) proxima(s) do vencimento.", proximas.size());
        proximas.forEach(o -> eventPublisher.publishEvent(new ObrigacaoVencendoEvent(
                o.getId(), o.getEmpresaId(), o.getClienteId(), o.getTipo(), o.getVencimento())));
    }
}
