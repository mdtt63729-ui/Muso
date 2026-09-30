# Round 179 (v0.5.196, code 203) — onboarding crash fix

Crash log paste-1-5.txt, on v0.5.195: IllegalArgumentException "Only
VectorDrawables and rasterized asset types are supported" from
SunnyIdentityPanel — the Round 173 onboarding change had pointed the
WELCOME page at R.drawable.splash_icon, which is a LAYER-LIST, and the
panel renders the icon with painterResource (vectors / PNG-JPG-WEBP
only). Every fresh install crashed on the onboarding screen.

Fix: the WELCOME page now uses R.drawable.small_icon — muso's
neon-waveform logo as a true vector drawable.
