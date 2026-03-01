/* ==========================================
   eRetain - Main JavaScript
   ========================================== */

(function() {
    'use strict';

    // Auto-dismiss notification toasts after 6 seconds (matches CSS timer animation)
    document.addEventListener('DOMContentLoaded', function() {
        var DISMISS_DELAY = 6000;
        var notifications = document.querySelectorAll('.notification-toast');
        notifications.forEach(function(toast) {
            // Pause timer on hover
            var dismissTimer;
            var remaining = DISMISS_DELAY;
            var startTime = Date.now();
            var timerBar = toast.querySelector('.notification-timer');

            function startDismiss() {
                startTime = Date.now();
                dismissTimer = setTimeout(function() {
                    var bsAlert = bootstrap.Alert.getOrCreateInstance(toast);
                    bsAlert.close();
                }, remaining);
                if (timerBar) {
                    timerBar.style.animationPlayState = 'running';
                }
            }

            toast.addEventListener('mouseenter', function() {
                clearTimeout(dismissTimer);
                remaining -= (Date.now() - startTime);
                if (remaining < 500) remaining = 500;
                if (timerBar) {
                    timerBar.style.animationPlayState = 'paused';
                }
            });

            toast.addEventListener('mouseleave', function() {
                startDismiss();
            });

            startDismiss();
        });

        // Also dismiss any non-toast alerts (legacy)
        var alerts = document.querySelectorAll('.alert-dismissible:not(.notification-toast)');
        alerts.forEach(function(alert) {
            setTimeout(function() {
                var bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
                bsAlert.close();
            }, 5000);
        });

        // Highlight active sidebar link based on current URL
        highlightActiveSidebarLink();

        // Initialize tooltips
        const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
        tooltipTriggerList.forEach(function(el) {
            new bootstrap.Tooltip(el);
        });

        // Initialize confirmation dialogs
        initDeleteConfirmations();
    });

    function highlightActiveSidebarLink() {
        const currentPath = window.location.pathname;
        const sidebarLinks = document.querySelectorAll('.sidebar .nav-link');
        
        sidebarLinks.forEach(function(link) {
            link.classList.remove('active');
            const href = link.getAttribute('href');
            if (href && currentPath.startsWith(href) && href !== '/') {
                link.classList.add('active');
            } else if (href === '/' && currentPath === '/') {
                link.classList.add('active');
            }
        });

        // If /dashboard is current path, activate dashboard link
        if (currentPath === '/dashboard' || currentPath === '/') {
            const dashLink = document.querySelector('.sidebar .nav-link[href="/dashboard"]');
            if (dashLink) dashLink.classList.add('active');
        }
    }

    function initDeleteConfirmations() {
        document.querySelectorAll('[data-confirm]').forEach(function(el) {
            el.addEventListener('click', function(e) {
                const message = this.getAttribute('data-confirm') || 'Are you sure you want to delete this item?';
                if (!confirm(message)) {
                    e.preventDefault();
                }
            });
        });
    }

    // Utility: Format date
    window.eRetainUtils = {
        formatDate: function(dateStr) {
            if (!dateStr) return '-';
            const date = new Date(dateStr);
            return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
        },
        formatHours: function(hours) {
            return hours != null ? hours.toFixed(1) : '0.0';
        }
    };

    // =================== Table Filter System ===================
    document.addEventListener('DOMContentLoaded', function() {
        const filterBar = document.querySelector('.filter-bar');
        if (!filterBar) return;

        const table = document.querySelector('.table');
        if (!table) return;

        const tbody = table.querySelector('tbody');
        if (!tbody) return;

        const rows = Array.from(tbody.querySelectorAll('tr'));
        const filterInputs = filterBar.querySelectorAll('[data-filter-col]');
        const clearBtn = filterBar.querySelector('.btn-clear');
        const countEl = document.getElementById('filter-count');

        function applyFilters() {
            let visibleCount = 0;
            rows.forEach(function(row) {
                let show = true;
                filterInputs.forEach(function(input) {
                    const colIndex = parseInt(input.getAttribute('data-filter-col'));
                    const cell = row.querySelectorAll('td')[colIndex];
                    if (!cell) return;
                    const cellText = cell.textContent.trim().toLowerCase();
                    const filterVal = input.value.trim().toLowerCase();
                    if (!filterVal) return;

                    if (input.tagName === 'SELECT') {
                        // Use contains matching for cells with multiple values (e.g. roles badges)
                        if (cellText.indexOf(filterVal) === -1) show = false;
                    } else if (input.type === 'date') {
                        // Date range filtering
                        const dateGroup = input.getAttribute('data-filter-date');
                        if (dateGroup) {
                            const fromInput = filterBar.querySelector('[data-filter-date="' + dateGroup + '"][data-filter-range="from"]');
                            const toInput = filterBar.querySelector('[data-filter-date="' + dateGroup + '"][data-filter-range="to"]');
                            // Only handle in 'from' input to avoid double processing
                            if (input.getAttribute('data-filter-range') === 'from') {
                                const fromVal = fromInput ? fromInput.value : '';
                                const toVal = toInput ? toInput.value : '';
                                if (fromVal && cellText < fromVal) show = false;
                                if (toVal && cellText > toVal) show = false;
                            }
                            // Skip 'to' input processing (handled by 'from')
                            if (input.getAttribute('data-filter-range') === 'to') return;
                        }
                    } else {
                        if (cellText.indexOf(filterVal) === -1) show = false;
                    }
                });
                row.style.display = show ? '' : 'none';
                if (show) visibleCount++;
            });

            if (countEl) {
                countEl.innerHTML = 'Showing <strong>' + visibleCount + '</strong> of <strong>' + rows.length + '</strong> records';
            }
        }

        filterInputs.forEach(function(input) {
            input.addEventListener('input', applyFilters);
            input.addEventListener('change', applyFilters);
        });

        if (clearBtn) {
            clearBtn.addEventListener('click', function() {
                filterInputs.forEach(function(input) {
                    input.value = '';
                });
                applyFilters();
            });
        }

        // Initial count
        if (countEl) {
            countEl.innerHTML = 'Showing <strong>' + rows.length + '</strong> of <strong>' + rows.length + '</strong> records';
        }
    });

})();
