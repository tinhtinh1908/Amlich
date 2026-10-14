# Support QR

Recipient values come from the author’s website source:
https://github.com/tinhtinh1908/home/blob/main/content/donate.js

The bundled image is the genuine VietQR compact template downloaded at the
user’s request from https://img.vietqr.io/image/970436-1032359704-compact.png.
It contains VietQR, NAPAS247 and bank branding without printed account details
or an amount. The app displays recipient information separately and uses this
same bundled PNG for both display and gallery saving; no runtime network is needed.

The payload is independently decoded and checked against the configured bank
BIN and account, with CRC16 validation and no fixed amount. The branded PNG is bundled directly; no local QR generator is required.

The banking-app transfer flow and gallery save require device verification.
