package com.gpiv.atlanticsprinttech.entities.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluaciones_radicacion")
public class EvaluacionRadicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "radicacion_id", nullable = false, unique = true)
    private RadicacionSolicitud radicacion;

    // Etapa 1 — Empleabilidad / Materia Prima / Reciclaje
    @Column(name = "etapa1_empleo_directo")
    private Integer etapa1EmpleoDirecto;

    @Column(name = "etapa1_materia_prima_local")
    private Integer etapa1MateriaPrimaLocal;

    @Column(name = "etapa1_impacto_ambiental")
    private Integer etapa1ImpactoAmbiental;

    @Column(name = "etapa1_observaciones", length = 500)
    private String etapa1Observaciones;

    @Column(name = "etapa1_fecha_guardado")
    private LocalDateTime etapa1FechaGuardado;

    // Etapa 2 — Financiero / Rentabilidad
    @Column(name = "etapa2_rentabilidad")
    private Integer etapa2Rentabilidad;

    @Column(name = "etapa2_solidez_financiera")
    private Integer etapa2SolidezFinanciera;

    @Column(name = "etapa2_inversion_declarada")
    private Integer etapa2InversionDeclarada;

    @Column(name = "etapa2_observaciones", length = 500)
    private String etapa2Observaciones;

    @Column(name = "etapa2_fecha_guardado")
    private LocalDateTime etapa2FechaGuardado;

    // Etapa 3 — Preadjudicación / Cronograma de Obra
    @Column(name = "etapa3_viabilidad_tecnica")
    private Integer etapa3ViabilidadTecnica;

    @Column(name = "etapa3_cronograma_obra")
    private Integer etapa3CronogramaObra;

    @Column(name = "etapa3_calidad_documentacion")
    private Integer etapa3CalidadDocumentacion;

    @Column(name = "etapa3_observaciones", length = 500)
    private String etapa3Observaciones;

    @Column(name = "etapa3_fecha_guardado")
    private LocalDateTime etapa3FechaGuardado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Column(name = "evaluador", length = 80)
    private String evaluador;

    protected EvaluacionRadicacion() {}

    private EvaluacionRadicacion(RadicacionSolicitud radicacion, String evaluador) {
        this.radicacion = radicacion;
        this.evaluador = evaluador;
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
    }

    public static EvaluacionRadicacion crear(RadicacionSolicitud radicacion, String evaluador) {
        return new EvaluacionRadicacion(radicacion, evaluador);
    }

    public void actualizarEtapa1(Integer empleo, Integer materiaPrima, Integer impactoAmbiental, String obs, String ev) {
        this.etapa1EmpleoDirecto = empleo;
        this.etapa1MateriaPrimaLocal = materiaPrima;
        this.etapa1ImpactoAmbiental = impactoAmbiental;
        this.etapa1Observaciones = obs;
        this.etapa1FechaGuardado = LocalDateTime.now();
        this.evaluador = ev;
        this.fechaActualizacion = LocalDateTime.now();
    }

    public void actualizarEtapa2(Integer rentabilidad, Integer solidez, Integer inversion, String obs, String ev) {
        this.etapa2Rentabilidad = rentabilidad;
        this.etapa2SolidezFinanciera = solidez;
        this.etapa2InversionDeclarada = inversion;
        this.etapa2Observaciones = obs;
        this.etapa2FechaGuardado = LocalDateTime.now();
        this.evaluador = ev;
        this.fechaActualizacion = LocalDateTime.now();
    }

    public void actualizarEtapa3(Integer viabilidad, Integer cronograma, Integer calidad, String obs, String ev) {
        this.etapa3ViabilidadTecnica = viabilidad;
        this.etapa3CronogramaObra = cronograma;
        this.etapa3CalidadDocumentacion = calidad;
        this.etapa3Observaciones = obs;
        this.etapa3FechaGuardado = LocalDateTime.now();
        this.evaluador = ev;
        this.fechaActualizacion = LocalDateTime.now();
    }

    public boolean etapa1Completa() {
        return etapa1EmpleoDirecto != null && etapa1MateriaPrimaLocal != null && etapa1ImpactoAmbiental != null;
    }

    public boolean etapa2Completa() {
        return etapa2Rentabilidad != null && etapa2SolidezFinanciera != null && etapa2InversionDeclarada != null;
    }

    public boolean etapa3Completa() {
        return etapa3ViabilidadTecnica != null && etapa3CronogramaObra != null && etapa3CalidadDocumentacion != null;
    }

    public Integer puntuacionEtapa1() {
        return etapa1Completa() ? etapa1EmpleoDirecto + etapa1MateriaPrimaLocal + etapa1ImpactoAmbiental : null;
    }

    public Integer puntuacionEtapa2() {
        return etapa2Completa() ? etapa2Rentabilidad + etapa2SolidezFinanciera + etapa2InversionDeclarada : null;
    }

    public Integer puntuacionEtapa3() {
        return etapa3Completa() ? etapa3ViabilidadTecnica + etapa3CronogramaObra + etapa3CalidadDocumentacion : null;
    }

    public Integer puntuacionTotal() {
        Integer p1 = puntuacionEtapa1();
        Integer p2 = puntuacionEtapa2();
        Integer p3 = puntuacionEtapa3();
        return (p1 != null && p2 != null && p3 != null) ? p1 + p2 + p3 : null;
    }

    public Long getId() { return id; }
    public RadicacionSolicitud getRadicacion() { return radicacion; }
    public Integer getEtapa1EmpleoDirecto() { return etapa1EmpleoDirecto; }
    public Integer getEtapa1MateriaPrimaLocal() { return etapa1MateriaPrimaLocal; }
    public Integer getEtapa1ImpactoAmbiental() { return etapa1ImpactoAmbiental; }
    public String getEtapa1Observaciones() { return etapa1Observaciones; }
    public LocalDateTime getEtapa1FechaGuardado() { return etapa1FechaGuardado; }
    public Integer getEtapa2Rentabilidad() { return etapa2Rentabilidad; }
    public Integer getEtapa2SolidezFinanciera() { return etapa2SolidezFinanciera; }
    public Integer getEtapa2InversionDeclarada() { return etapa2InversionDeclarada; }
    public String getEtapa2Observaciones() { return etapa2Observaciones; }
    public LocalDateTime getEtapa2FechaGuardado() { return etapa2FechaGuardado; }
    public Integer getEtapa3ViabilidadTecnica() { return etapa3ViabilidadTecnica; }
    public Integer getEtapa3CronogramaObra() { return etapa3CronogramaObra; }
    public Integer getEtapa3CalidadDocumentacion() { return etapa3CalidadDocumentacion; }
    public String getEtapa3Observaciones() { return etapa3Observaciones; }
    public LocalDateTime getEtapa3FechaGuardado() { return etapa3FechaGuardado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public String getEvaluador() { return evaluador; }
}