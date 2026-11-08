# Interior node actions

A tap on an existing interior node opens Convertir en esquina / Convertir en
curva and Borrar punto. Endpoints and newly inserted nodes retain their existing
interaction. Screen-space touch slop distinguishes taps from drags. Each action
records one geometry-history step and checks that its target is still current.
Pixels change only on confirmation. Long-press tension editing remains available.
