(() => {
    const modal = document.getElementById('authModal');
    const frame = document.getElementById('authModalFrame');
    const btnClose = document.getElementById('authModalClose');
    const triggers = document.querySelectorAll('[data-auth-modal]');
    const DURACION_CIERRE_MS = 220;

    let temporizadorCierre = null;

    if (!modal || !frame || !btnClose || !triggers.length) {
        return;
    }

    frame.addEventListener('load', () => {
        frame.classList.add('auth-modal__frame--ready');
    });

    function abrirModal(url) {
        if (temporizadorCierre) {
            window.clearTimeout(temporizadorCierre);
            temporizadorCierre = null;
        }
        frame.classList.remove('auth-modal__frame--ready');
        frame.src = url;
        modal.classList.add('auth-modal--open');
        modal.setAttribute('aria-hidden', 'false');
        document.body.classList.add('auth-modal-open');
        btnClose.focus();
    }

    function cerrarModal() {
        if (!modal.classList.contains('auth-modal--open')) {
            return;
        }

        modal.classList.remove('auth-modal--open');

        if (temporizadorCierre) {
            window.clearTimeout(temporizadorCierre);
        }

        temporizadorCierre = window.setTimeout(() => {
            modal.setAttribute('aria-hidden', 'true');
            document.body.classList.remove('auth-modal-open');
            frame.classList.remove('auth-modal__frame--ready');
            frame.src = 'about:blank';
            temporizadorCierre = null;
        }, DURACION_CIERRE_MS);
    }

    triggers.forEach((trigger) => {
        trigger.addEventListener('click', (event) => {
            const destino = trigger.getAttribute('data-auth-modal');
            if (!destino) {
                return;
            }
            event.preventDefault();
            abrirModal(destino);
        });
    });

    btnClose.addEventListener('click', cerrarModal);

    modal.addEventListener('click', (event) => {
        if (event.target === modal) {
            cerrarModal();
        }
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && modal.classList.contains('auth-modal--open')) {
            cerrarModal();
        }
    });
})();