const CONFIG = {
  TCHAD_MAROC: {
    label:      'Tchad → Maroc',
    deviseSrc:  'FCFA',
    deviseDest: 'MAD',
    commission: 0.03,
    taux:       64,
    tauxLabel:  'Commission 3 % · 64 FCFA = 1 MAD'
  },
  MAROC_TCHAD: {
    label:      'Maroc → Tchad',
    deviseSrc:  'MAD',
    deviseDest: 'FCFA',
    taux:       61,
    tauxLabel:  '1 MAD = 61 FCFA'
  },
  FRANCE_TCHAD: {
    label:      'France → Tchad',
    deviseSrc:  'EUR',
    deviseDest: 'FCFA',
    taux:       655,
    tauxLabel:  '1 EUR = 655 FCFA'
  },
  TCHAD_FRANCE: {
    label:      'Tchad → France',
    deviseSrc:  'FCFA',
    deviseDest: 'EUR',
    taux:       700,
    tauxLabel:  '700 FCFA = 1 EUR'
  }
};

function arrondir(valeur) {
  const entier   = Math.floor(valeur);
  const decimale = valeur - entier;
  return decimale >= 0.5 ? entier + 1 : entier;
}

// "1 000 000" → "1000000"  (tous types d'espaces)
function deformater(s) {
  if (!s) return '';
  return s.replace(/\s+/g, '').replace(',', '.');
}

// 1000000 → "1 000 000"
function formater(n) {
  if (n === null || n === undefined || n === '' || isNaN(n)) return '';
  return n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
}

/**
 * Formate le champ en temps réel.
 * Préserve la position du curseur en comptant les chiffres avant lui,
 * puis replace le curseur après le même nb de chiffres dans la valeur formatée.
 */
function formatInputTempsReel(input, source) {
  const selStart = input.selectionStart;
  const brut     = input.value;

  // Nb de chiffres avant le curseur (on ignore les espaces)
  const digitsAvant = (brut.slice(0, selStart).match(/\d/g) || []).length;

  // Ne garder que les chiffres
  const chiffresSeuls = brut.replace(/\D/g, '');

  const formate = chiffresSeuls === ''
    ? ''
    : parseInt(chiffresSeuls, 10).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ' ');

  input.value = formate;

  // Replacer le curseur au bon endroit
  let nouveauPos   = formate.length;
  let compteDigits = 0;
  for (let i = 0; i < formate.length; i++) {
    if (/\d/.test(formate[i])) compteDigits++;
    if (compteDigits === digitsAvant) { nouveauPos = i + 1; break; }
  }
  if (digitsAvant === 0) nouveauPos = 0;
  input.setSelectionRange(nouveauPos, nouveauPos);

  mettreAJour(source);
}

function calcSrcToDest(sens, montant) {
  const cfg = CONFIG[sens];
  if (sens === 'TCHAD_MAROC')  return montant * (1 - cfg.commission) / cfg.taux;
  if (sens === 'MAROC_TCHAD')  return montant * cfg.taux;
  if (sens === 'FRANCE_TCHAD') return montant * cfg.taux;
  if (sens === 'TCHAD_FRANCE') return montant / cfg.taux;
}

function calcDestToSrc(sens, montant) {
  const cfg = CONFIG[sens];
  if (sens === 'TCHAD_MAROC')  return montant * cfg.taux / (1 - cfg.commission);
  if (sens === 'MAROC_TCHAD')  return montant / cfg.taux;
  if (sens === 'FRANCE_TCHAD') return montant / cfg.taux;
  if (sens === 'TCHAD_FRANCE') return montant * cfg.taux;
}

function mettreAJour(source) {
  const sens   = document.getElementById('sens').value;
  const cfg    = CONFIG[sens];
  const inSrc  = document.getElementById('montantSrc');
  const inDest = document.getElementById('montantDest');
  const erreur = document.getElementById('zoneErreur');

  document.getElementById('badgeSrc').textContent  = cfg.deviseSrc;
  document.getElementById('badgeDest').textContent = cfg.deviseDest;
  inSrc.placeholder  = 'Montant en ' + cfg.deviseSrc;
  inDest.placeholder = 'Montant en ' + cfg.deviseDest;
  document.getElementById('rateLabel').textContent = cfg.tauxLabel;

  erreur.style.display = 'none';
  erreur.textContent   = '';

  const raw = deformater(source === 'src' ? inSrc.value : inDest.value);

  if (raw === '') {
    if (source === 'src') inDest.value = '';
    else inSrc.value = '';
    return;
  }

  const montant = parseFloat(raw);

  if (isNaN(montant) || montant < 0) {
    erreur.innerHTML     = '<i class="bi bi-exclamation-circle-fill"></i> Veuillez entrer un montant valide et positif.';
    erreur.style.display = 'flex';
    if (source === 'src') inDest.value = '';
    else inSrc.value = '';
    return;
  }

  if (montant === 0) {
    if (source === 'src') inDest.value = formater(0);
    else inSrc.value = formater(0);
    return;
  }

  if (source === 'src') {
    inDest.value = formater(arrondir(calcSrcToDest(sens, montant)));
  } else {
    inSrc.value = formater(arrondir(calcDestToSrc(sens, montant)));
  }
}

document.addEventListener('DOMContentLoaded', function () {
  const selectSens = document.getElementById('sens');
  const inSrc      = document.getElementById('montantSrc');
  const inDest     = document.getElementById('montantDest');

  selectSens.addEventListener('change', function () {
    inSrc.value  = '';
    inDest.value = '';
    mettreAJour('src');
  });

  inSrc.addEventListener('input',  function () { formatInputTempsReel(inSrc,  'src');  });
  inDest.addEventListener('input', function () { formatInputTempsReel(inDest, 'dest'); });

  mettreAJour('src');
});

// ─── CHATBOT ────────────────────────────────────────────
(function () {
  var toggle   = document.getElementById('chatbotToggle');
  var panel    = document.getElementById('chatbotPanel');
  var closeBtn = document.getElementById('chatbotClose');
  var messages = document.getElementById('chatbotMessages');
  var input    = document.getElementById('chatbotInput');
  var sendBtn  = document.getElementById('chatbotSend');

  if (!toggle) return;

  function openPanel() {
    panel.classList.add('chatbot-open');
    panel.setAttribute('aria-hidden', 'false');
    input.focus();
  }

  function closePanel() {
    panel.classList.remove('chatbot-open');
    panel.setAttribute('aria-hidden', 'true');
  }

  toggle.addEventListener('click', function () {
    panel.classList.contains('chatbot-open') ? closePanel() : openPanel();
  });
  closeBtn.addEventListener('click', closePanel);

  function appendMessage(text, type) {
    var div = document.createElement('div');
    div.className = 'chatbot-msg chatbot-msg-' + type;
    div.textContent = text;
    messages.appendChild(div);
    messages.scrollTop = messages.scrollHeight;
    return div;
  }

  function send() {
    var question = input.value.trim();
    if (!question) return;

    appendMessage(question, 'user');
    input.value = '';
    input.disabled = true;
    sendBtn.disabled = true;

    var loading = appendMessage('En cours…', 'loading');

    fetch('/api/simulateur/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ question: question })
    })
      .then(function (res) { return res.json(); })
      .then(function (data) {
        loading.remove();
        appendMessage(data.reponse || 'Aucune réponse.', 'bot');
      })
      .catch(function () {
        loading.remove();
        appendMessage('Erreur de connexion. Veuillez réessayer.', 'bot');
      })
      .finally(function () {
        input.disabled = false;
        sendBtn.disabled = false;
        input.focus();
      });
  }

  sendBtn.addEventListener('click', send);
  input.addEventListener('keydown', function (e) {
    if (e.key === 'Enter') send();
  });
})();
