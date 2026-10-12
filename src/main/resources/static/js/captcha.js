(() => {
  "use strict";
  const newElement = (tag, className, text) => {
    const el = document.createElement(tag);
    if (className) el.className = className;
    if (text !== undefined) el.textContent = text;
    return el;
  };

  const setup = (widget) => {
    const form = widget.closest("form");
    const stage = widget.querySelector(".captcha-stage");
    const prompt = widget.querySelector(".captcha-prompt");
    const feedback = widget.querySelector(".captcha-feedback");
    const challengeId = widget.querySelector('[name="captchaId"]');
    const answerField = widget.querySelector('[name="captchaAnswer"]');
    const refresh = widget.querySelector(".captcha-refresh");
    let version = 0;

    const setAnswer = (answer) => { answerField.value = answer; };
    function renderIcons(challenge) {
      stage.classList.add("captcha-icon-stage");
      const board = newElement("img", "captcha-icon-board");
      board.src = challenge.boardUrl;
      board.alt = "Nine illustrated icons in a 3 by 3 grid, with a cat in the background";
      stage.append(board);
      const grid = newElement("div", "captcha-icon-grid");
      const picked = [];
      for (const item of [...challenge.choices].sort((a, b) => a.slot - b.slot)) {
        const button = newElement("button", "captcha-icon-choice", "");
        button.type = "button";
        button.setAttribute("aria-label", `Choose symbol at row ${Math.floor(item.slot / 3) + 1}, column ${item.slot % 3 + 1}`);
        button.setAttribute("aria-pressed", "false");
        button.addEventListener("click", () => {
          if (picked.length >= 3 || picked.some(p => p.id === item.id)) return;
          picked.push(item);
          button.classList.add("captcha-picked");
          button.setAttribute("aria-pressed", "true");
          feedback.textContent = `${picked.length} of 3 selected`;
          if (picked.length === 3) {
            setAnswer(picked.map(p => p.id).join(","));
            feedback.textContent = "Three icons selected. Submit or clear the choices to try again.";
          }
        });
        grid.append(button);
      }
      stage.append(grid);
      const clear = newElement("button", "captcha-small-button", "Clear choices");
      clear.type = "button";
      clear.addEventListener("click", () => {
        picked.length = 0; setAnswer(""); feedback.textContent = "Selections cleared.";
        grid.querySelectorAll("button").forEach(b => { b.classList.remove("captcha-picked"); b.setAttribute("aria-pressed", "false"); });
      });
      stage.append(clear);
    }

    function renderRotate(challenge) {
      const spin = newElement("div", "captcha-rotate-stage");
      const board = newElement("img", "captcha-rotate-board");
      board.src = challenge.boardUrl;
      board.alt = "Fish and three red crossed-out decoys positioned around the cat";
      spin.append(board);
      const cat = newElement("img", "captcha-spinning-cat");
      cat.src = challenge.pieceUrl;
      cat.alt = "Rotatable cat; head points toward the cat's natural upright direction";
      let turns = 0;
      const redraw = () => { cat.style.transform = `rotate(${turns * 90}deg)`; };
      redraw(); spin.append(cat); stage.append(spin);
      const actions = newElement("div", "captcha-spin-actions");
      const left = newElement("button", "captcha-small-button", "↶ Turn left");
      const right = newElement("button", "captcha-small-button", "↷ Turn right");
      [left, right].forEach(b => { b.type = "button"; actions.append(b); });
      const rotate = (change) => {
        turns = (turns + change + 4) % 4;
        redraw(); setAnswer(String(turns));
        feedback.textContent = "Cat rotated. Submit when its head points toward the fish.";
      };
      left.addEventListener("click", () => rotate(-1));
      right.addEventListener("click", () => rotate(1));
      stage.append(actions);
    }

    function renderMatchingCats(challenge) {
      stage.classList.add("captcha-match-stage");
      const board = newElement("img", "captcha-match-board");
      board.src = challenge.boardUrl;
      board.alt = "Nine cat portraits on different backgrounds, shown in a three by three grid";
      stage.append(board);
      const grid = newElement("div", "captcha-match-grid");
      const picked = new Set();
      const buttons = new Map();
      const update = () => {
        setAnswer(picked.size === 3 ? Array.from(picked).join(",") : "");
        feedback.textContent = picked.size === 3
          ? "Three panels selected. Submit or change your choices."
          : `${picked.size} of 3 matching cats selected`;
        for (const [id, button] of buttons) {
          const selected = picked.has(id);
          button.classList.toggle("captcha-picked", selected);
          button.setAttribute("aria-pressed", String(selected));
        }
      };
      for (const item of [...challenge.choices].sort((a, b) => a.slot - b.slot)) {
        const button = newElement("button", "captcha-match-choice", "");
        button.type = "button";
        button.setAttribute("aria-label", `Cat at row ${Math.floor(item.slot / 3) + 1}, column ${item.slot % 3 + 1}`);
        button.addEventListener("click", () => {
          if (picked.has(item.id)) picked.delete(item.id);
          else if (picked.size < 3) picked.add(item.id);
          update();
        });
        buttons.set(item.id, button);
        grid.append(button);
      }
      stage.append(grid);
      const clear = newElement("button", "captcha-small-button", "Clear cat choices");
      clear.type = "button";
      clear.addEventListener("click", () => { picked.clear(); update(); });
      stage.append(clear);
      update();
    }

    async function load() {
      const requestVersion = ++version;
      refresh.disabled = true; setAnswer(""); challengeId.value = "";
      prompt.textContent = "Loading a verification challenge…";
      stage.className = "captcha-stage"; stage.replaceChildren();
      feedback.textContent = "";
      try {
        const url = new URL(widget.dataset.challengeUrl, window.location.href);
        url.searchParams.set("scope", widget.dataset.captchaScope);
        const response = await fetch(url, { credentials: "same-origin", cache: "no-store" });
        if (!response.ok) throw new Error("Could not load verification challenge.");
        const challenge = await response.json();
        if (requestVersion !== version) return;
        challengeId.value = challenge.id;
        prompt.textContent = challenge.prompt;
        switch (challenge.kind) {
          case "ICON_ORDER": renderIcons(challenge); break;
          case "ROTATE_TO_FOOD": renderRotate(challenge); break;
          case "MATCHING_CATS": renderMatchingCats(challenge); break;
          default: throw new Error("Unrecognized CAPTCHA challenge");
        }
      } catch {
        if (requestVersion === version) prompt.textContent = "Could not load verification. Check the connection and try again.";
      } finally { if (requestVersion === version) refresh.disabled = false; }
    }

    refresh.addEventListener("click", load);
    if (form) form.addEventListener("submit", (event) => {
      if (!challengeId.value || !answerField.value) {
        event.preventDefault();
        feedback.textContent = "Complete the verification challenge before submitting.";
      }
    });
    void load();
  };

  document.querySelectorAll(".cats-captcha").forEach(setup);
})();
