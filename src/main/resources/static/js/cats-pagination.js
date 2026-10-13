/* CATS pagination: a progressive enhancement. Existing server-rendered links
 * remain functional when JavaScript is disabled. */
(() => {
  'use strict';
  const MAX_VISIBLE = 7;
  const PARAMS = ['attendancePage', 'budgetPage', 'claimsPage', 'pageNum', 'page'];

  const arrow = (direction, href, label) => {
    const enabled = Boolean(href);
    const control = document.createElement(enabled ? 'a' : 'span');
    control.className = 'cats-pager-arrow' + (enabled ? '' : ' is-disabled');
    if (enabled) {
      control.href = href;
      control.setAttribute('aria-label', label);
      control.title = label;
    } else control.setAttribute('aria-hidden', 'true');
    const icon = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    icon.setAttribute('viewBox', '0 0 24 24');
    icon.setAttribute('aria-hidden', 'true');
    icon.setAttribute('focusable', 'false');
    const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    path.setAttribute('d', direction === 'left' ? 'M15 18 9 12l6-6' : 'm9 18 6-6-6-6');
    icon.append(path);
    control.append(icon);
    return control;
  };

  const paginationLinks = (root) => [...root.querySelectorAll('a[href]')];
  const labelIs = (link, pattern) => pattern.test((link.textContent || '').trim());
  const numberInfo = (root, links) => {
    const raw = (root.textContent || '').replace(/\s+/g, ' ').trim();
    const counter = raw.match(/(?:page\s+)?(\d+)\s*(?:of|\/)\s*(\d+)/i);
    if (counter) return { current: Number(counter[1]), total: Number(counter[2]) };
    // The administration catalogue renders numbered links but no summary.
    const nums = links.map((a) => /^\d+$/.test(a.textContent.trim()) ? Number(a.textContent.trim()) : 0);
    const total = Math.max(...nums, 0);
    const prev = links.find((a) => labelIs(a, /^(?:prev(?:ious)?(?:\s+page)?)$/i));
    const next = links.find((a) => labelIs(a, /^next(?:\s+page)?$/i));
    const fromPrev = prev && Number(new URL(prev.href, document.baseURI).searchParams.get('pageNum'));
    const fromNext = next && Number(new URL(next.href, document.baseURI).searchParams.get('pageNum'));
    const current = Number.isInteger(fromPrev) && prev ? fromPrev + 2
      : Number.isInteger(fromNext) && next ? fromNext
      : Number(new URLSearchParams(location.search).get('pageNum') || '0') + 1;
    return { current, total };
  };

  function enhance(root) {
    if (root.dataset.catsPagerReady === 'true') return;
    const links = paginationLinks(root);
    const prev = links.find((a) => labelIs(a, /^(?:prev(?:ious)?(?:\s+page)?)$/i));
    const next = links.find((a) => labelIs(a, /^next(?:\s+page)?$/i));
    const isNumbered = links.some((a) => /^\d+$/.test(a.textContent.trim()));
    if (!prev && !next && !isNumbered && !/\bpage\b|\d+\s*\/\s*\d+/i.test(root.textContent)) return;

    const { current, total } = numberInfo(root, links);
    if (!Number.isSafeInteger(current) || !Number.isSafeInteger(total) ||
        total < 1 || total > 100000 || current < 1 || current > total) return;

    const label = root.getAttribute('aria-label') || 'Table pages';
    const numericLink = links.find((a) => /^\d+$/.test(a.textContent.trim()));
    const seed = next || prev || numericLink;
    const fromUrl = seed ? new URL(seed.href, document.baseURI) : new URL(document.baseURI);
    const candidate = PARAMS.filter((key) => fromUrl.searchParams.has(key));
    const desired = next ? current : prev ? current - 2 : current - 1;
    const matches = candidate.filter((key) => Number(fromUrl.searchParams.get(key)) === desired);
    const preferred = /attendance/i.test(label) ? 'attendancePage'
      : /budget/i.test(label) ? 'budgetPage'
      : /claim/i.test(label) && /report/i.test(location.pathname) ? 'claimsPage'
      : /claim/i.test(label) ? 'page' : null;
    const param = (preferred && matches.includes(preferred) ? preferred : null)
      || matches[0]
      || (isNumbered ? 'pageNum' : null)
      || candidate[0]
      || (/\/claims(?:\/|$)/.test(location.pathname) ? 'page' : 'pageNum');

    const makeHref = (index) => {
      const url = new URL(fromUrl.href);
      url.searchParams.set(param, String(index));
      return url.pathname + url.search + url.hash;
    };

    // A bounded window keeps the playful pagination legible on phones, while
    // all page links remain meaningful even with thousands of results.
    const count = Math.min(MAX_VISIBLE, total);
    const start = Math.max(1, Math.min(current - Math.floor(count / 2), total - count + 1));
    const end = start + count - 1;

    const brand = document.createElement('span');
    brand.className = 'cats-pager-brand';
    brand.setAttribute('aria-hidden', 'true');
    brand.textContent = 'C';

    const strip = document.createElement('ol');
    strip.className = 'cats-pager-pages';
    for (let n = start; n <= end; n++) {
      const li = document.createElement('li');
      const active = n === current;
      const el = document.createElement(active ? 'span' : 'a');
      el.className = 'cats-pager-page' + (active ? ' is-current' : '');
      if (active) {
        el.setAttribute('aria-current', 'page');
        el.setAttribute('aria-label', `Current page, ${n}`);
      } else {
        el.href = makeHref(n - 1);
        el.setAttribute('aria-label', `Go to page ${n}`);
      }
      const letter = document.createElement('span');
      letter.className = 'cats-pager-letter';
      letter.setAttribute('aria-hidden', 'true');
      letter.textContent = 'a';
      const number = document.createElement('span');
      number.className = 'cats-pager-number';
      number.textContent = String(n);
      el.append(letter, number);
      li.append(el);
      strip.append(li);
    }
    const endBrand = document.createElement('span');
    endBrand.className = 'cats-pager-tail';
    endBrand.setAttribute('aria-hidden', 'true');
    endBrand.textContent = end < total ? '…aats' : 'ts';

    const status = document.createElement('span');
    status.className = 'cats-pager-sr';
    status.textContent = `Page ${current} of ${total}`;
    root.classList.add('cats-google-pager');
    root.setAttribute('aria-label', label);
    if (root.tagName !== 'NAV') root.setAttribute('role', 'navigation');
    root.replaceChildren(
      arrow('left', current > 1 ? makeHref(current - 2) : null, 'Previous page'),
      brand, strip, endBrand,
      arrow('right', current < total ? makeHref(current) : null, 'Next page'),
      status
    );
    root.dataset.catsPagerReady = 'true';
  }

  function fillEmptyTables() {
    // The application has many table components. Keep the headings visible on
    // an empty result, even when a particular template has no explicit row.
    document.querySelectorAll('table').forEach((table) => {
      if (table.closest('#dp') || table.dataset.noEmptyState === 'true') return;
      const head = table.tHead;
      const body = table.tBodies[0];
      if (!head || !body || body.rows.length) return;
      const row = body.insertRow();
      row.className = 'table-empty-row';
      const cell = row.insertCell();
      cell.colSpan = Math.max(1, head.rows[0]?.cells.length || 1);
      const box = document.createElement('div');
      box.className = 'table-empty-state';
      const cat = document.createElement('img');
      cat.src = '/images/cats/tearful.png';
      cat.alt = '';
      const message = document.createElement('p');
      message.textContent = 'No records on this page yet.';
      box.append(cat, message);
      cell.append(box);
    });
  }

  const init = () => {
    fillEmptyTables();
    const candidates = new Set(document.querySelectorAll('nav.pagination, .pagination, nav[aria-label$="pages"]'));
    // Some legacy claim tables use an unlabelled <nav>; only enhance one
    // containing actual pagination controls, not the site navigation.
    document.querySelectorAll('nav').forEach((nav) => {
      if (/\bpage\s*\d+\s*of\s*\d+\b/i.test(nav.textContent || '')) candidates.add(nav);
    });
    candidates.forEach(enhance);
  };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init, { once: true });
  else init();
})();
