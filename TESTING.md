# NetNotice verification

The redesign was checked with Java 21 on Windows. All three suites passed.

| Suite | Coverage |
| --- | --- |
| Member3IntegrationTest | Multiple clients, announcements, live votes, vote changes, disconnect cleanup, late join, final poll results |
| FunctionalRegressionTest | Duplicate names, UTF-8 and multiline announcements/replies, pipe characters in poll fields, invalid votes, votes after close, poll validation, network-level quiz privacy including late join, three server restarts, sound PCM and mute, UTF-8 export |
| DesktopWorkflowTest | Real Swing buttons over TCP: start/stop, join/leave, roster/search, notices, clipboard, local read state, replies, polls/quizzes, final results, three reconnects, history search, native export dialogs for history/results/logs, preview dialog, sound volume dialog, clear draft; renders every page at 1100 and 600 pixel window widths and checks input bounds |

Run `Test-NetNotice.ps1` to reproduce. Generated screenshots and the soft-bell WAV are in `out/previews` (ignored build output).

The notification uses an original 650 ms two-tone PCM bell. Tests verify nonzero audio samples, duration, and silent output at zero volume; perceived loudness and physical speaker output still depend on the selected Windows audio device. Connection → Notifications includes a preview control.

LAN firewall/router behaviour, macOS font rendering, screen-reader behaviour, and hardware audio have not been verified on additional machines. Visual QA covers Windows compact and wide layouts. Notification history and read status are local to the current client session. Quiz results are response counts; automatic grading is not provided.

The desktop workflow checks toolbar delivery on both sides, collapse and reopen, dismissal, inbox navigation, and message retention while minimized. It checks the font loaded on Windows and verifies that notifications belong to their workspace frame.
