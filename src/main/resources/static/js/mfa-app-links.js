(() => {
  'use strict';
  const android = /Android/i.test(navigator.userAgent);
  const mobile = /Android|iPhone|iPad|iPod/i.test(navigator.userAgent);
  if (mobile) {
    for (const section of document.querySelectorAll('[data-mobile-enroll-actions]')) {
      section.hidden = false;
    }
  }
  if (!android) return;
  for (const info of document.querySelectorAll('[data-desktop-mfa-info]')) {
    info.hidden = true;
  }

  for (const section of document.querySelectorAll('[data-android-mfa-actions]')) {
    section.hidden = false;
  }

  const appPackages = Object.freeze({
    google: 'com.google.android.apps.authenticator2',
    microsoft: 'com.azure.authenticator'
  });
  const choice = document.querySelector('[data-authenticator-choice]');
  const openAuth = document.querySelector('[data-open-authenticator]');
  if (choice && openAuth) {
    const saved = sessionStorage.getItem('cats-authenticator-choice');
    if (saved && appPackages[saved]) choice.value = saved;
    const update = () => {
      const app = appPackages[choice.value];
      if (!app) return;
      const store = encodeURIComponent(`https://play.google.com/store/apps/details?id=${app}`);
      openAuth.href = `intent:#Intent;action=android.intent.action.MAIN;category=android.intent.category.LAUNCHER;package=${app};S.browser_fallback_url=${store};end`;
      sessionStorage.setItem('cats-authenticator-choice', choice.value);
    };
    choice.addEventListener('change', update);
    update();
  }

  for (const anchor of document.querySelectorAll('[data-open-cats-mobile]')) {
    const seedUri = anchor.getAttribute('data-provision-uri');
    const entry = seedUri ? `enroll?uri=${encodeURIComponent(seedUri)}` : 'authenticator';
    const fallback = encodeURIComponent(new URL('/mobile/download', location.origin).href);
    anchor.href = `intent://${entry}#Intent;scheme=catsmobile;package=sg.edu.iss.cats.mobile;S.browser_fallback_url=${fallback};end`;
  }
})();
