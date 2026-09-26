package com.danza_check.demo.repository;

import java.util.List;

import com.danza_check.demo.entity.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

	boolean existsBySesionIdAndCorreo(Long sesionId, String correo);

	List<Asistencia> findBySesionIdOrderByFechaHoraAscIdAsc(Long sesionId);

	long countBySesionId(Long sesionId);

}
