package com.danza_check.demo.repository;

import java.util.List;
import java.util.Optional;

import com.danza_check.demo.entity.SesionAsistencia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SesionAsistenciaRepository extends JpaRepository<SesionAsistencia, Long> {

	Optional<SesionAsistencia> findByCodigo(String codigo);

	boolean existsByCodigo(String codigo);

	List<SesionAsistencia> findAllByOrderByFechaDescCreatedAtDesc();

}
