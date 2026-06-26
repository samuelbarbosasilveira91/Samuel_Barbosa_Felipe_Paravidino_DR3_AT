package com.marketplace.card.kafka;

import com.marketplace.card.event.CompraRealizadaEvent;
import com.marketplace.card.model.Card;
import com.marketplace.card.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseConsumer {

    private final CardRepository cardRepository;

    @KafkaListener(topics = "trades.purchases", groupId = "card-service-group")
    public void consumePurchase(CompraRealizadaEvent event) {
        log.info("Evento CompraRealizada recebido do Kafka: {}", event);

        try {
            Optional<Card> optionalCard = cardRepository.findById(event.getCardId());

            if (optionalCard.isPresent()) {
                Card card = optionalCard.get();
                log.info("Atualizando preço médio de '{}'. Preço antigo: {}, Preço de venda: {}",
                        card.getName(), card.getAveragePrice(), event.getPrice());

                double currentAvg = card.getAveragePrice() != null ? card.getAveragePrice() : 0.0;
                double newAvg = Math.round(((currentAvg + event.getPrice()) / 2.0) * 100.0) / 100.0;
                card.setAveragePrice(newAvg);

                Card saved = cardRepository.save(card);
                log.info("Preço médio do card '{}' atualizado para: {}", saved.getName(), saved.getAveragePrice());
            } else {
                log.warn("Card com ID '{}' não encontrado no banco de dados.", event.getCardId());
            }
        } catch (DataAccessException | IllegalArgumentException e) {
            log.error(" Erro (Banco de Dados ou Argumento Inválido) ao consumir evento do Kafka para cardId '{}': {}", event.getCardId(), e.getMessage());
        }
    }
}
