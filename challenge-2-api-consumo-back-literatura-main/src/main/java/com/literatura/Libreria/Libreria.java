package com.literatura.Libreria;
import com.literatura.config.ConsumoApiGutendex;
import com.literatura.config.ConvertirDatos;
import com.literatura.models.Autor;
import com.literatura.models.Libro;
import com.literatura.models.LibrosRespuestaApi;
import com.literatura.models.records.DatosLibro;
import com.literatura.repository.iAutorRepository;
import com.literatura.repository.iLibroRepository;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
public class Libreria {

    private final Scanner sc = new Scanner(System.in);
    private final ConsumoApiGutendex consumoApi = new ConsumoApiGutendex();
    private final ConvertirDatos convertir = new ConvertirDatos();
    private static final String API_BASE = "https://gutendex.com/books/?search=";
    private final iLibroRepository libroRepository;
    private final iAutorRepository autorRepository;

    public Libreria(iLibroRepository libroRepository, iAutorRepository autorRepository) {
        this.libroRepository = libroRepository;
        this.autorRepository = autorRepository;
    }

    public void iniciar() {
        while (true) {
            mostrarMenu();
            int opcion = solicitarOpcion();
            if (opcion == 0) {
                System.out.println("¡Gracias por usar la librería! Hasta pronto.");
                break;
            }
            procesarOpcion(opcion);
        }
    }

    private void mostrarMenu() {
        System.out.println("""
                
                |***************************************************|
                |*****     BIENVENIDO A LIBRERÍA STIFLER      ******|
                |***************************************************|
                1 - Agregar Libro por Nombre
                2 - Listar Libros Guardados
                3 - Buscar Libro por Nombre
                4 - Listar Autores
                5 - Buscar Autores por Año
                6 - Buscar Libros por Idioma
                7 - Top 10 Libros Más Descargados
                8 - Buscar Autor por Nombre
                0 - Salir
                |***************************************************|
                """);
    }

    private int solicitarOpcion() {
        System.out.print("Seleccione una opción: ");
        while (!sc.hasNextInt()) {
            System.out.println("Por favor, ingrese un número válido.");
            sc.next(); // Limpia entrada inválida
        }
        return sc.nextInt();
    }

    private void procesarOpcion(int opcion) {
        sc.nextLine(); // Limpia buffer
        switch (opcion) {
            case 1 -> agregarLibroPorNombre();
            case 2 -> listarLibrosGuardados();
            case 3 -> buscarLibroPorNombre();
            case 4 -> listarAutores();
            case 5 -> buscarAutoresPorAnio();
            case 6 -> buscarLibrosPorIdioma();
            case 7 -> listarTop10LibrosMasDescargados();
            case 8 -> buscarAutorPorNombre();
            default -> System.out.println("Opción no válida. Intente nuevamente.");
        }
    }

    private void agregarLibroPorNombre() {
        System.out.print("Ingrese el nombre del libro: ");
        String nombreLibro = sc.nextLine().trim().toLowerCase();
        String json = consumoApi.obtenerDatos(API_BASE + nombreLibro.replace(" ", "%20"));
        LibrosRespuestaApi datos = convertir.convertirDatosJsonAJava(json, LibrosRespuestaApi.class);

        if (datos == null || datos.getResultadoLibros().isEmpty()) {
            System.out.println("No se encontraron resultados.");
            return;
        }

        DatosLibro primerLibro = datos.getResultadoLibros().get(0);
        Libro libro = new Libro(primerLibro);

        if (libroRepository.existsByTitulo(libro.getTitulo())) {
            System.out.println("El libro ya existe en la base de datos.");
        } else {
            libroRepository.save(libro);
            System.out.println("Libro agregado: " + libro);
        }
    }

    private void listarLibrosGuardados() {
        List<Libro> libros = libroRepository.findAll();
        if (libros.isEmpty()) {
            System.out.println("No hay libros guardados.");
        } else {
            System.out.println("Libros guardados:");
            libros.forEach(System.out::println);
        }
    }

    private void buscarLibroPorNombre() {
        System.out.print("Ingrese el título del libro: ");
        String titulo = sc.nextLine().trim();
        Libro libro = libroRepository.findByTituloContainsIgnoreCase(titulo);
        if (libro == null) {
            System.out.println("No se encontró un libro con el título '" + titulo + "'.");
        } else {
            System.out.println("Libro encontrado: " + libro);
        }
    }

    private void listarAutores() {
        List<Autor> autores = autorRepository.findAll();
        if (autores.isEmpty()) {
            System.out.println("No hay autores registrados.");
        } else {
            System.out.println("Autores registrados:");
            autores.stream()
                    .map(Autor::getNombre)
                    .distinct()
                    .forEach(System.out::println);
        }
    }

    private void buscarAutoresPorAnio() {
        System.out.print("Ingrese el año: ");
        int anio = sc.nextInt();
        List<Autor> autores = autorRepository.findByCumpleaniosLessThanOrFechaFallecimientoGreaterThanEqual(anio, anio);
        if (autores.isEmpty()) {
            System.out.println("No se encontraron autores vivos en el año " + anio + ".");
        } else {
            System.out.println("Autores vivos en " + anio + ":");
            autores.forEach(autor -> System.out.println(autor.getNombre()));
        }
    }

    private void buscarLibrosPorIdioma() {
        System.out.print("Ingrese el idioma (es/en): ");
        String idioma = sc.nextLine().trim();
        List<Libro> libros = libroRepository.findByIdioma(idioma);
        if (libros.isEmpty()) {
            System.out.println("No se encontraron libros en el idioma '" + idioma + "'.");
        } else {
            System.out.println("Libros en " + idioma + ":");
            libros.forEach(System.out::println);
        }
    }

    private void listarTop10LibrosMasDescargados() {
        List<Libro> topLibros = libroRepository.findTop10ByTituloByCantidadDescargas();
        if (topLibros.isEmpty()) {
            System.out.println("No se encontraron libros descargados.");
        } else {
            System.out.println("Top 10 libros más descargados:");
            for (int i = 0; i < topLibros.size(); i++) {
                Libro libro = topLibros.get(i);
                System.out.printf("%d. %s (Descargas: %d)\n", i + 1, libro.getTitulo(), libro.getCantidadDescargas());
            }
        }
    }

    private void buscarAutorPorNombre() {
        System.out.print("Ingrese el nombre del autor: ");
        String nombre = sc.nextLine().trim();
        Optional<Autor> autor = autorRepository.findFirstByNombreContainsIgnoreCase(nombre);
        if (autor.isPresent()) {
            System.out.println("Autor encontrado: " + autor.get().getNombre());
        } else {
            System.out.println("No se encontró un autor con el nombre '" + nombre + "'.");
        }
    }
}
