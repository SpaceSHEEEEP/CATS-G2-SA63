(() => {
  'use strict';
  const element = (tag, cls, label) => {
    const node = document.createElement(tag);
    if (cls) node.className = cls;
    if (label !== undefined) node.textContent = label;
    return node;
  };

  function setup(dialog) {
    const form = dialog.closest('form');
    if (!form || typeof dialog.showModal !== 'function') return;
    const stage = dialog.querySelector('.captcha-stage');
    const prompt = dialog.querySelector('.captcha-prompt');
    const feedback = dialog.querySelector('.captcha-feedback');
    const challengeId = dialog.querySelector('[name="captchaId"]');
    const answer = dialog.querySelector('[name="captchaAnswer"]');
    const refresh = dialog.querySelector('.captcha-refresh');
    const confirm = dialog.querySelector('.captcha-confirm');
    const cancel = dialog.querySelector('.captcha-cancel');
    let generation = 0;
    let submitting = false;
    let submitter = null;

    const setAnswer = (value) => { answer.value = value; };
    function renderIcons(challenge) {
      const frame = element('div', 'captcha-icon-stage');
      const board = element('img', 'captcha-icon-board');
      board.src = challenge.boardUrl;
      board.alt = 'Cat illustration with nine differently sized, scattered objects';
      board.draggable = false;
      frame.append(board);
      const markers = element('div', 'captcha-icon-markers');
      markers.setAttribute('aria-hidden', 'true');
      frame.append(markers);
      const hitArea = element('button', 'captcha-icon-surface');
      hitArea.type = 'button';
      hitArea.setAttribute('aria-label', 'Select three objects in the requested order by tapping the cat illustration');
      hitArea.title = 'Tap the objects in the requested order';
      frame.append(hitArea);
      const chosen = [];
      const cursor = element('span', 'captcha-icon-cursor');
      markers.append(cursor);
      let keyboardX = 0.5, keyboardY = 0.5;

      function pick(x, y) {
        if (chosen.length >= 3 || !Number.isFinite(x) || !Number.isFinite(y)) return;
        x = Math.max(0, Math.min(1, x));
        y = Math.max(0, Math.min(1, y));
        chosen.push(`${Math.round(x * 1000)}:${Math.round(y * 1000)}`);
        const drop = element('span', 'captcha-click-marker');
        drop.style.left = `${x * 100}%`;
        drop.style.top = `${y * 100}%`;
        drop.append(element('span', 'captcha-click-number', String(chosen.length)));
        markers.append(drop);
        setAnswer(chosen.length === 3 ? chosen.join(',') : '');
        feedback.textContent = chosen.length === 3
          ? 'Three positions marked. Verify to continue, or clear and try again.'
          : `${chosen.length} of 3 positions marked.`;
      }
      hitArea.addEventListener('click', (event) => {
        if (event.detail === 0) return; // keyboard uses the crosshair instead
        const rect = hitArea.getBoundingClientRect();
        if (rect.width && rect.height) {
          pick((event.clientX - rect.left) / rect.width, (event.clientY - rect.top) / rect.height);
        }
      });
      hitArea.addEventListener('keydown', (event) => {
        const delta = 0.035;
        if (event.key === 'ArrowLeft') keyboardX = Math.max(0, keyboardX - delta);
        else if (event.key === 'ArrowRight') keyboardX = Math.min(1, keyboardX + delta);
        else if (event.key === 'ArrowUp') keyboardY = Math.max(0, keyboardY - delta);
        else if (event.key === 'ArrowDown') keyboardY = Math.min(1, keyboardY + delta);
        else if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault(); pick(keyboardX, keyboardY); return;
        } else return;
        event.preventDefault();
        cursor.style.left = `${keyboardX * 100}%`;
        cursor.style.top = `${keyboardY * 100}%`;
        cursor.style.display = 'block';
      });
      stage.append(frame);
      const clear = element('button', 'captcha-small-button', 'Clear choices');
      clear.type = 'button';
      clear.addEventListener('click', () => {
        chosen.length = 0; setAnswer('');
        markers.querySelectorAll('.captcha-click-marker').forEach((node) => node.remove());
        feedback.textContent = 'Selections cleared.';
      });
      stage.append(clear);
    }

    function renderRotation(challenge) {
      const frame = element('div', 'captcha-rotate-stage');
      const board = element('img', 'captcha-rotate-board');
      board.src = challenge.boardUrl;
      board.alt = 'One fish and three distracting objects around the cat';
      frame.append(board);
      const cat = element('img', 'captcha-spinning-cat');
      cat.src = challenge.pieceUrl;
      cat.alt = 'Rotatable cat';
      let turns = 0;
      const redraw = () => { cat.style.transform = `rotate(${turns * 90}deg)`; };
      redraw(); frame.append(cat); stage.append(frame);
      const actions = element('div', 'captcha-spin-actions');
      for (const [direction, label] of [[-1, '↶ Turn left'], [1, '↷ Turn right']]) {
        const button = element('button', 'captcha-small-button', label);
        button.type = 'button';
        button.addEventListener('click', () => {
          turns = (turns + direction + 4) % 4;
          redraw(); setAnswer(String(turns));
          feedback.textContent = 'Position updated. Verify when the cat faces the fish.';
        });
        actions.append(button);
      }
      stage.append(actions);
    }

    function renderMatching(challenge) {
      const frame = element('div', 'captcha-match-stage');
      const board = element('img', 'captcha-match-board');
      board.src = challenge.boardUrl;
      board.alt = 'Nine illustrated cat portraits in a three by three grid';
      frame.append(board);
      const grid = element('div', 'captcha-match-grid');
      const selected = new Set();
      const buttons = new Map();
      const update = () => {
        setAnswer(selected.size === 3 ? [...selected].join(',') : '');
        feedback.textContent = selected.size === 3
          ? 'Three cats chosen. Verify or change your selection.'
          : `${selected.size} of 3 cats selected.`;
        for (const [id, button] of buttons) {
          button.classList.toggle('captcha-picked', selected.has(id));
          button.setAttribute('aria-pressed', String(selected.has(id)));
        }
      };
      for (const choice of [...challenge.choices].sort((a, b) => a.slot - b.slot)) {
        const button = element('button', 'captcha-match-choice');
        button.type = 'button';
        button.setAttribute('aria-label', `Cat at row ${Math.floor(choice.slot / 3) + 1}, column ${choice.slot % 3 + 1}`);
        button.addEventListener('click', () => {
          if (selected.has(choice.id)) selected.delete(choice.id);
          else if (selected.size < 3) selected.add(choice.id);
          update();
        });
        buttons.set(choice.id, button); grid.append(button);
      }
      frame.append(grid); stage.append(frame);
      const clear = element('button', 'captcha-small-button', 'Clear cat choices');
      clear.type = 'button';
      clear.addEventListener('click', () => { selected.clear(); update(); });
      stage.append(clear);
      update();
    }

    async function load() {
      const current = ++generation;
      refresh.disabled = true;
      confirm.disabled = true;
      challengeId.value = '';
      setAnswer('');
      prompt.textContent = 'Loading verification challenge…';
      feedback.textContent = '';
      stage.className = 'captcha-stage';
      stage.replaceChildren();
      try {
        const url = new URL(dialog.dataset.challengeUrl, window.location.href);
        url.searchParams.set('scope', dialog.dataset.captchaScope);
        const response = await fetch(url, { credentials: 'same-origin', cache: 'no-store' });
        if (!response.ok) throw new Error(`Verification service unavailable (${response.status}).`);
        const challenge = await response.json();
        if (current !== generation || !dialog.open) return;
        challengeId.value = challenge.id;
        prompt.textContent = challenge.prompt;
        if (challenge.kind === 'ICON_ORDER') renderIcons(challenge);
        else if (challenge.kind === 'ROTATE_TO_FOOD') renderRotation(challenge);
        else if (challenge.kind === 'MATCHING_CATS') renderMatching(challenge);
        else throw new Error('Unsupported verification challenge.');
        confirm.disabled = false;
      } catch (error) {
        if (current === generation && dialog.open) {
          prompt.textContent = error instanceof Error ? error.message : 'Verification unavailable.';
        }
      } finally { if (current === generation) refresh.disabled = false; }
    }

    refresh.addEventListener('click', () => { void load(); });
    cancel.addEventListener('click', () => { dialog.close(); });
    dialog.addEventListener('close', () => {
      generation++;
      if (!submitting) { challengeId.value = ''; setAnswer(''); submitter = null; }
    });
    form.addEventListener('submit', (event) => {
      if (submitting) return;
      event.preventDefault();
      submitter = event.submitter;
      if (!dialog.open) {
        dialog.showModal();
        void load(); // no CAPTCHA requests are made until form submission
      }
    });
    confirm.addEventListener('click', () => {
      if (!challengeId.value || !answer.value) {
        feedback.textContent = 'Complete the challenge before continuing.';
        return;
      }
      submitting = true;
      dialog.close();
      // requestSubmit retains browser validation, CSRF fields and submit-button semantics.
      form.requestSubmit(submitter || undefined);
    });
  }

  document.querySelectorAll('dialog.cats-captcha').forEach(setup);
})();
