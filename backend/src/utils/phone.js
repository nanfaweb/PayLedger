/**
 * Normalize Pakistani mobile numbers to E.164: +92XXXXXXXXXX
 */
function normalizePakistaniPhone(input) {
  if (input == null) return null;
  const raw = String(input).trim().replace(/[\s\-()]/g, '');

  let digits = raw;
  if (digits.startsWith('+')) {
    digits = `+${digits.slice(1).replace(/\D/g, '')}`;
  } else {
    digits = digits.replace(/\D/g, '');
  }

  let national;
  if (digits.startsWith('+92')) {
    national = digits.slice(3);
  } else if (digits.startsWith('92') && digits.length >= 12) {
    national = digits.slice(2);
  } else if (digits.startsWith('0')) {
    national = digits.slice(1);
  } else {
    national = digits;
  }

  if (!/^[3][0-9]{9}$/.test(national)) {
    return null;
  }

  return `+92${national}`;
}

function isValidPakistaniPhone(input) {
  return normalizePakistaniPhone(input) !== null;
}

module.exports = { normalizePakistaniPhone, isValidPakistaniPhone };
