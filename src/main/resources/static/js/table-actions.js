(() => {
  'use strict';
  const dialog = document.getElementById('cats-action-confirm-dialog');
  const form = document.getElementById('cats-action-confirm-form');
  const phraseInput = document.getElementById('cats-action-confirm-input');
  const actionButton = document.getElementById('cats-action-confirm-submit');
  const extraWrapper = document.getElementById('cats-action-extra-wrapper');
  const extraInput = document.getElementById('cats-action-extra-input');
  let sourceButton = null;
  let expectedPhrase = '';

  const refresh = () => {
    if (!actionButton || !phraseInput) return;
    const extraValid = !extraWrapper || extraWrapper.hidden || !!extraInput.value.trim();
    actionButton.disabled = phraseInput.value === expectedPhrase ? !extraValid : true;
  };
  if (dialog && form && phraseInput && actionButton && extraWrapper && extraInput) {
    phraseInput.addEventListener('input', refresh);
    extraInput.addEventListener('input', refresh);
    document.addEventListener('click', (event) => {
      const trigger = event.target.closest('.js-confirm-action');
      if (!trigger) return;
      event.preventDefault();
      if (trigger.closest('[popover]:popover-open')) trigger.closest('[popover]').hidePopover();
      const action = trigger.dataset.confirmAction;
      if (!action) return;
      const parsed = new URL(action, location.href);
      if (parsed.origin !== location.origin) return; // never post to a remote origin
      sourceButton = trigger;
      form.action = parsed.href;
      expectedPhrase = trigger.dataset.confirmPhrase || '';
      document.getElementById('cats-action-confirm-title').textContent =
        trigger.dataset.confirmTitle || 'Confirm action';
      document.getElementById('cats-action-confirm-subject').textContent =
        trigger.dataset.confirmSubject || '';
      document.getElementById('cats-action-confirm-message').textContent =
        trigger.dataset.confirmMessage || 'Please verify this action.';
      document.getElementById('cats-action-confirm-phrase').textContent = expectedPhrase;
      actionButton.textContent = trigger.dataset.confirmButton || 'Confirm action';
      actionButton.classList.toggle('danger', trigger.dataset.confirmDanger !== 'false');
      phraseInput.value = '';
      extraInput.value = '';
      extraWrapper.hidden = !trigger.dataset.confirmExtra;
      extraInput.disabled = extraWrapper.hidden;
      extraInput.required = !extraWrapper.hidden;
      extraInput.name = trigger.dataset.confirmExtra || '';
      extraInput.maxLength = Number(trigger.dataset.confirmExtraMax || 255);
      document.getElementById('cats-action-extra-label').textContent = trigger.dataset.confirmExtraLabel || 'Additional details';
      refresh();
      dialog.showModal();
      phraseInput.focus();
    });
    document.getElementById('cats-action-confirm-cancel').addEventListener('click', () => dialog.close());
    dialog.addEventListener('close', () => {
      phraseInput.value = '';
      extraInput.value = '';
      actionButton.disabled = true;
      if (sourceButton && sourceButton.isConnected) sourceButton.focus();
    });
    form.addEventListener('submit', (event) => {
      refresh();
      if (actionButton.disabled) event.preventDefault();
    });
  }

  document.querySelectorAll('.row-action-menu[popover]').forEach((menu) => {
    menu.addEventListener('toggle', () => {
      if (!menu.matches(':popover-open')) return;
      const toggle = [...document.querySelectorAll('button[popovertarget]')]
        .find(button => button.getAttribute('popovertarget') === menu.id);
      if (!toggle) return;
      const rect = toggle.getBoundingClientRect();
      const width = menu.getBoundingClientRect().width || 216;
      const left = Math.max(8, Math.min(window.innerWidth - width - 8, rect.right - width));
      menu.style.left = left + 'px';
      // Flip above the trigger when there is insufficient room below.
      const height = menu.getBoundingClientRect().height;
      const y = rect.bottom + 6 + height > window.innerHeight
        ? Math.max(8, rect.top - height - 6) : rect.bottom + 6;
      menu.style.top = y + 'px';
    });
  });

  document.addEventListener('click', (event) => {
    const opener = event.target.closest('.js-open-edit-dialog');
    if (opener) {
      const target = document.getElementById(opener.dataset.dialogTarget || '');
      if (target && typeof target.showModal === 'function') {
        event.preventDefault();
        target.showModal();
        target.querySelector('input:not([type="hidden"]), textarea, button')?.focus();
      }
    }
    const close = event.target.closest('.js-close-edit-dialog');
    if (close) close.closest('dialog')?.close();
  });
})();
