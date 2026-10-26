# Wand selection confirmation

SelectionConfirmation owns the pending movement session. Confirm keeps pixels and
clears the selection. Cancel undoes only the movement checkpoints recorded since
this wand selection started, restoring overwritten destination pixels as well as
the original source; then it clears the selection. Rollback runs on the worker
and publishes one completed frame. Revision checks prevent rewinding unrelated
edits. Switching to a drawing tool ends the pending movement session but keeps
the selection mask available to that tool. A new wand selection starts a new
session. SelectionConfirmationControls owns the independent toolbar buttons.
Line/Curve code and native history implementations are unchanged.
