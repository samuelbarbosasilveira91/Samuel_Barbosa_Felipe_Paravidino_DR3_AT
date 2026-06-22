package com.marketplace.card.kafka;

import com.marketplace.card.event.CompraRealizadaEvent;
import com.marketplace.card.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseConsumer {

    private final CardRepository cardRepository;

    @KafkaListener(topics = "trades.purchases", groupId = "card-service-group")
    public void consumePurchase(CompraRealizadaEvent event) {
        String cid = event.getCorrelationId();
        if (cid != null) {
            MDC.put("correlationId", cid);
        }

        log.info("Evento CompraRealizada recebido do Kafka: {}", event);

        try {
            cardRepository.findById(event.getCardId())
                    .flatMap(card -> {
                        log.info("Atualizando preço médio de '{}'. Preço antigo: {}, Preço de venda: {}",
                                card.getName(), card.getAveragePrice(), event.getPrice());

                        double currentAvg = card.getAveragePrice() != null ? card.getAveragePrice() : 0.0;
                        double newAvg = Math.round(((currentAvg + event.getPrice()) / 2.0) * 100.0) / 100.0;
                        card.setAveragePrice(newAvg);

                        return cardRepository.save(card);
                    })
                    .doOnSuccess(saved -> log.info("Preço médio do card '{}' atualizado para: {}", saved.getName(), saved.getAveragePrice()))
                    .doOnError(err -> log.error("Falha ao processar evento de compra: {}", err.getMessage()))
                    .block();
        } catch (Exception e) {
            log.error("Erro ao consumir evento do Kafka para cardId '{}': {}", event.getCardId(), e.getMessage());
        } finally {
            MDC.remove("correlationId");
        }
    }
}
