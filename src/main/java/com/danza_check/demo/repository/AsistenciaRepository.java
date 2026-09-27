package com.danza_check.demo.repository;

import java.util.List;
import java.util.Optional;

import com.danza_check.demo.entity.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

	boolean existsBySesionIdAndCorreo(Long sesionId, String correo);

	List<Asistencia> findBySesionIdOrderByFechaHoraAscIdAsc(Long sesionId);

	long countBySesionId(Long sesionId);

	/**
	 * Busca la asistencia acotando por sesión. Sin este filtro, un id de
	 * asistencia ajeno podría borrarse desde la URL de otra sesión.
	 */
	Optional<Asistencia> findByIdAndSesionId(Long id, Long sesionId);

}
