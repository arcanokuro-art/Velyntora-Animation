# Interior node actions

CurvePointMenu distinguishes a single tap on an existing interior node from a
drag, using screen-space touch slop and tracking the entire gesture. The popup
is positioned next to that node and exposes Borrar punto. Endpoints and freshly
inserted nodes never open this menu. Removal keeps coordinate/tension arrays in
sync and records one geometry-history step. Native pixels are not changed until
the existing curve-confirm action. Existing long-press tension editing remains.
