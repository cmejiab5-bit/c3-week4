package gt.edu.uinsight.alert.repository;

import gt.edu.uinsight.alert.entity.AlertStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertStatusHistoryRepository
        extends JpaRepository<AlertStatusHistory, Long> {

    List<AlertStatusHistory> findByAlertIdOrderByChangedAtAscIdAsc(Long alertId);
}
