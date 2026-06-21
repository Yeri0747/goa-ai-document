package es.upm.api.data.daos;

import es.upm.api.data.entities.Invoice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends MongoRepository<Invoice, String> {
    Optional<Invoice> findByDocumentId(String documentId);
}
