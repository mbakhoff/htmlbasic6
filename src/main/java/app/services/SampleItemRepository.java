package app.services;

import app.model.SampleItem;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SampleItemRepository extends CrudRepository<SampleItem, Long> {

}
