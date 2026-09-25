package com.example.aiapp.transaction.service;

import com.example.aiapp.transaction.config.TransactionProperties;
import com.example.aiapp.transaction.dao.ShipmentDao;
import com.example.aiapp.transaction.entity.ShipmentEntity;
import com.example.aiapp.transaction.entity.ShipmentStatus;
import com.example.aiapp.transaction.exception.ShippingUnavailableException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The last participant before the pivot. A failure here is the interesting case: the order
 * exists, the stock is reserved and the customer has already been charged, so all three
 * earlier steps have to be undone in reverse.
 */
@Service
@RequiredArgsConstructor
public class ShippingServiceImpl implements ShippingService {

    private static final Logger log = LoggerFactory.getLogger(ShippingServiceImpl.class);

    private final ShipmentDao shipmentDao;
    private final TransactionProperties properties;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long scheduleShipment(UUID sagaId, String orderRef, String destinationCountry,
                                 String shippingAddress) {
        Set<String> serviceable = properties.getShipping().getServiceableCountries();
        String country = destinationCountry == null
                ? "" : destinationCountry.trim().toUpperCase(Locale.ROOT);

        boolean served = serviceable.stream()
                .anyMatch(c -> c != null && c.trim().equalsIgnoreCase(country));
        if (!served) {
            throw new ShippingUnavailableException(orderRef, country, serviceable);
        }

        Instant now = Instant.now();
        ShipmentEntity shipment = ShipmentEntity.builder()
                .sagaId(sagaId.toString())
                .trackingNumber("TRK-" + sagaId)
                .orderRef(orderRef)
                .destinationCountry(country)
                .shippingAddress(shippingAddress)
                .status(ShipmentStatus.SCHEDULED)
                .scheduledAt(now)
                .build();

        Long shipmentId = shipmentDao.save(shipment).getId();
        log.info("[saga={}] SCHEDULE_SHIPMENT: booked shipment to {} for order {} (shipmentId={})",
                sagaId, country, orderRef, shipmentId);
        return shipmentId;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelShipment(Long shipmentId) {
        if (shipmentId == null) {
            return;
        }
        Optional<ShipmentEntity> found = shipmentDao.findById(shipmentId);
        if (found.isEmpty()) {
            log.warn("COMPENSATE cancelShipment: shipment id={} no longer exists - nothing to undo",
                    shipmentId);
            return;
        }
        ShipmentEntity shipment = found.get();
        if (shipment.getStatus() != ShipmentStatus.SCHEDULED) {
            log.debug("COMPENSATE cancelShipment: shipment id={} already {} - idempotent no-op",
                    shipmentId, shipment.getStatus());
            return;
        }

        shipment.setStatus(ShipmentStatus.CANCELLED);
        shipment.setCancelledAt(Instant.now());
        shipmentDao.save(shipment);
        log.info("COMPENSATE cancelShipment: shipment id={} -> CANCELLED", shipmentId);
    }
}
