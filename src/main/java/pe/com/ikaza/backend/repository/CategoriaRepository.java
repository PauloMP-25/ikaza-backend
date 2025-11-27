package pe.com.ikaza.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.com.ikaza.backend.entity.Categoria;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    /**
     * Busca una categoría por su nombre
     */
    Optional<Categoria> findByNombreCategoria(String nombreCategoria);

    /**
     * Busca todas las categorías activas
     * Ordena por nombre alfabéticamente
     */
    List<Categoria> findByActivoTrueOrderByNombreCategoriaAsc();

    /**
     * Verifica si existe una categoría con ese nombre
     */
    boolean existsByNombreCategoria(String nombreCategoria);

    /**
     * Busca categorías por nombre que contenga un texto (búsqueda parcial)
     */
    List<Categoria> findByNombreCategoriaContainingIgnoreCase(String nombre);
}