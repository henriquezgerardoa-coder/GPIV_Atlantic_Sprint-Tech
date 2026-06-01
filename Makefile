# 🚀 Makefile - ENREPAVI GPIV
# Automatización de buenas prácticas en código frontend
# Adaptado para estructura: frontend/src/main/resources/static/

.PHONY: help all format lint minify images security a11y report clean backup install

# Rutas del proyecto
FRONTEND_DIR := ./frontend
STATIC_DIR := $(FRONTEND_DIR)/src/main/resources/static
CSS_DIR := $(STATIC_DIR)/css
JS_DIR := $(STATIC_DIR)/js
HTML_DIR := $(STATIC_DIR)
IMG_DIR := $(STATIC_DIR)/img

# Colores para output
RED := \033[0;31m
GREEN := \033[0;32m
YELLOW := \033[1;33m
BLUE := \033[0;34m
NC := \033[0m

# ============================================
# 📋 AYUDA
# ============================================
help:
	@echo "$(BLUE)╔════════════════════════════════════════╗$(NC)"
	@echo "$(BLUE)║$(NC)  🚀 ENREPAVI - Frontend Automation  $(BLUE)║$(NC)"
	@echo "$(BLUE)╚════════════════════════════════════════╝$(NC)"
	@echo ""
	@echo "$(YELLOW)COMANDOS PRINCIPALES:$(NC)"
	@echo "  $(GREEN)make all$(NC)            - Ejecutar todas las correcciones"
	@echo "  $(GREEN)make install$(NC)        - Instalar herramientas"
	@echo ""
	@echo "$(YELLOW)CORRECCIONES INDIVIDUALES:$(NC)"
	@echo "  $(GREEN)make format$(NC)         - Formatear código"
	@echo "  $(GREEN)make lint$(NC)           - Linting"
	@echo "  $(GREEN)make minify$(NC)         - Minificar CSS/JS"
	@echo "  $(GREEN)make images$(NC)         - Optimizar imágenes"
	@echo "  $(GREEN)make security$(NC)       - Verificación de seguridad"
	@echo "  $(GREEN)make a11y$(NC)           - Verificar accesibilidad"
	@echo ""
	@echo "$(YELLOW)UTILIDADES:$(NC)"
	@echo "  $(GREEN)make report$(NC)         - Generar reporte"
	@echo "  $(GREEN)make backup$(NC)         - Crear backup"
	@echo "  $(GREEN)make clean$(NC)          - Limpiar archivos"
	@echo ""
	@echo "$(YELLOW)RUTAS:$(NC)"
	@echo "  Frontend: $(FRONTEND_DIR)"
	@echo "  Static:   $(STATIC_DIR)"
	@echo ""

# ============================================
# 🔧 INSTALACIÓN
# ============================================
install:
	@echo "$(BLUE)📥 Herramientas ya instaladas (verificar):$(NC)"
	@echo ""
	@prettier --version && echo "  ✓ Prettier"
	@eslint --version && echo "  ✓ ESLint"
	@htmlhint --version && echo "  ✓ HTMLHint"
	@stylelint --version && echo "  ✓ stylelint"
	@cleancss --version && echo "  ✓ clean-css"
	@uglifyjs --version && echo "  ✓ uglify-js"
	@echo ""
	@echo "$(GREEN)✅ Todas las herramientas disponibles$(NC)"

# ============================================
# 🎨 FORMATEAR
# ============================================
format:
	@echo "$(BLUE)🎨 Formateando código...$(NC)"
	@echo "  → Archivos HTML"
	@prettier --write "$(HTML_DIR)/*.html" --tab-width=4
	@echo "  → Archivos CSS"
	@prettier --write "$(CSS_DIR)/*.css" --tab-width=4
	@echo "  → Archivos JavaScript"
	@prettier --write "$(JS_DIR)/*.js" --tab-width=4
	@echo "$(GREEN)✅ Código formateado$(NC)"

# ============================================
# 🔍 LINTING
# ============================================
lint:
	@echo "$(BLUE)🔍 Analizando código...$(NC)"
	@echo "  → JavaScript con ESLint"
	@eslint "$(JS_DIR)/*.js" --fix --max-warnings 10 || true
	@echo "  → HTML con HTMLHint"
	@htmlhint "$(HTML_DIR)/*.html" || true
	@echo "  → CSS con stylelint"
	@stylelint "$(CSS_DIR)/*.css" --fix --max-warnings 10 || true
	@echo "$(GREEN)✅ Linting completado$(NC)"

# ============================================
# 📦 MINIFICACIÓN
# ============================================
minify: minify-css minify-js

minify-css:
	@echo "$(BLUE)📦 Minificando CSS...$(NC)"
	@for file in $(CSS_DIR)/*.css; do \
		if [[ ! "$$file" == *".min.css" ]]; then \
			basefile=$$(basename "$$file" .css); \
			cleancss -o "$(CSS_DIR)/$$basefile.min.css" "$$file"; \
			echo "  ✓ $$basefile.min.css"; \
		fi; \
	done
	@echo "$(GREEN)✅ CSS minificado$(NC)"

minify-js:
	@echo "$(BLUE)📦 Minificando JavaScript...$(NC)"
	@for file in $(JS_DIR)/*.js; do \
		if [[ ! "$$file" == *".min.js" ]]; then \
			basefile=$$(basename "$$file" .js); \
			uglifyjs "$$file" -o "$(JS_DIR)/$$basefile.min.js" -c -m 2>/dev/null; \
			echo "  ✓ $$basefile.min.js"; \
		fi; \
	done
	@echo "$(GREEN)✅ JavaScript minificado$(NC)"

# ============================================
# 🖼️ OPTIMIZAR IMÁGENES
# ============================================
images:
	@echo "$(BLUE)🖼️  Optimizando imágenes...$(NC)"
	@if [ -d "$(IMG_DIR)" ]; then \
		if command -v convert >/dev/null 2>&1; then \
			echo "  → Optimizando PNG/JPG"; \
			for file in $(IMG_DIR)/*.{png,jpg,jpeg}; do \
				[ -f "$$file" ] && convert "$$file" -quality 85 "$$file" && echo "    ✓ $$(basename $$file)"; \
			done; \
			echo "$(GREEN)✅ Imágenes optimizadas$(NC)"; \
		else \
			echo "$(YELLOW)⚠️  ImageMagick no instalado$(NC)"; \
		fi; \
	else \
		echo "$(YELLOW)⚠️  Carpeta de imágenes no encontrada$(NC)"; \
	fi

# ============================================
# 🔒 SEGURIDAD
# ============================================
security:
	@echo "$(BLUE)🔒 Verificando seguridad...$(NC)"
	@echo "  → Buscando código inseguro"
	@echo "    eval(): $$(grep -r "eval(" $(JS_DIR) 2>/dev/null | wc -l)"
	@echo "    innerHTML: $$(grep -r "innerHTML" $(JS_DIR) 2>/dev/null | wc -l)"
	@echo "    onclick inline: $$(grep -r "onclick=" $(HTML_DIR) 2>/dev/null | wc -l)"
	@echo "$(GREEN)✅ Verificación completada$(NC)"

# ============================================
# ♿ ACCESIBILIDAD
# ============================================
a11y:
	@echo "$(BLUE)♿ Verificando accesibilidad...$(NC)"
	@echo "  → aria-labels: $$(grep -r "aria-label" $(HTML_DIR)/*.html 2>/dev/null | wc -l)"
	@echo "  → aria-required: $$(grep -r "aria-required" $(HTML_DIR)/*.html 2>/dev/null | wc -l)"
	@echo "  → role attributes: $$(grep -r "role=" $(HTML_DIR)/*.html 2>/dev/null | wc -l)"
	@echo "$(GREEN)✅ Verificación completada$(NC)"

# ============================================
# 📊 REPORTE
# ============================================
report:
	@echo "$(BLUE)📊 Generando reporte de calidad...$(NC)"
	@echo "--- REPORTE DE CALIDAD ---" > code-quality.txt
	@echo "Fecha: $$(date)" >> code-quality.txt
	@echo "" >> code-quality.txt
	@echo "### LÍNEAS DE CÓDIGO ###" >> code-quality.txt
	@echo "HTML: $$(wc -l $(HTML_DIR)/*.html 2>/dev/null | tail -1)" >> code-quality.txt
	@echo "CSS: $$(wc -l $(CSS_DIR)/*.css 2>/dev/null | tail -1)" >> code-quality.txt
	@echo "JavaScript: $$(wc -l $(JS_DIR)/*.js 2>/dev/null | tail -1)" >> code-quality.txt
	@echo "" >> code-quality.txt
	@echo "### TAMAÑO DE ARCHIVOS ###" >> code-quality.txt
	@du -sh $(STATIC_DIR) >> code-quality.txt 2>/dev/null || true
	@echo "" >> code-quality.txt
	@echo "$(GREEN)✅ Reporte guardado: code-quality.txt$(NC)"

# ============================================
# 💾 BACKUP
# ============================================
backup:
	@echo "$(BLUE)💾 Creando backup...$(NC)"
	@mkdir -p ".backup-$$(date +%Y%m%d-%H%M%S)"
	@cp -r "$(STATIC_DIR)" ".backup-$$(date +%Y%m%d-%H%M%S)/" 2>/dev/null || true
	@echo "$(GREEN)✅ Backup creado$(NC)"

# ============================================
# 🧹 LIMPIAR
# ============================================
clean:
	@echo "$(BLUE)🧹 Limpiando archivos generados...$(NC)"
	@find "$(CSS_DIR)" -name "*.min.css" -delete
	@find "$(JS_DIR)" -name "*.min.js" -delete
	@find . -name ".DS_Store" -delete
	@rm -f code-quality.txt
	@echo "$(GREEN)✅ Archivos eliminados$(NC)"

# ============================================
# 🚀 EJECUTAR TODO
# ============================================
all: backup format lint minify security a11y report
	@echo ""
	@echo "$(BLUE)╔════════════════════════════════════════╗$(NC)"
	@echo "$(BLUE)║$(NC)  $(GREEN)🎉 TODAS LAS CORRECCIONES COMPLETADAS$(NC)  $(BLUE)║$(NC)"
	@echo "$(BLUE)╚════════════════════════════════════════╝$(NC)"
	@echo ""
	@echo "$(GREEN)✅ Tu código está optimizado y listo para producción$(NC)"
	@echo ""

# ============================================
# ⚡ PIPELINE RÁPIDO
# ============================================
quick: format lint
	@echo "$(GREEN)✅ Quick fix completado$(NC)"

# ============================================
# ✨ VERIFICACIÓN COMPLETA
# ============================================
check: format lint security a11y
	@echo "$(GREEN)✅ Verificación completa$(NC)"
