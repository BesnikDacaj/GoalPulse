# GoalPulse — Peer Reviews (Deliverable 6, Part B)

**Course:** ISTE-330
**Submission date:** 2026-05-09

Each member answers, for every other team member, the rubric question:

> *"Would you want to work with this person again — yes/no and why?"*

Reviews are signed and intentionally direct so the instructor can see honest feedback.

---

## Reviewer: Mateo Josipović

**On Petar Marinović — Yes.**
Petar drove the entire frontend and was unblocked by simple API contracts; once `/api/v1` endpoints stabilized he iterated on UI without supervision. He responds to design feedback fast and is comfortable with both vanilla JS and React/TS.

**On Ana Kovačić — Yes.**
Ana caught two portability issues (MySQL reserved word, CHECK syntax) before they hit the demo. She also wrote the database dictionary herself, which is normally the worst chore on a database project. I would put her on the schema design of any future project.

**On Luka Horvat — Yes.**
Luka took ownership of QA early and treated acceptance tests as a first-class artifact, not an afterthought. The fallback path for football-data.org being properly tested is his work, and that single feature saves the live demo if the lab Wi-Fi is bad.

---

## Reviewer: Petar Marinović

**On Mateo Josipović — Yes.**
Mateo's REST envelope (`success`, `data`, `message`) was stable from M3 onward, so I never had to rewire frontend calls because of backend changes. He merged PRs the same day and did not block frontend work waiting on backend reviews.

**On Ana Kovačić — Yes.**
Ana gave me realistic seed data (real teams, real founders, plausible coach names) which made the UI look credible during demos instead of "Team A vs Team B." Small detail, big impact.

**On Luka Horvat — Yes.**
Luka caught a mobile-overlap bug at <360px width that I had missed. I want him on the QA side of any future project.

---

## Reviewer: Ana Kovačić

**On Mateo Josipović — Yes.**
Mateo treated the schema as the source of truth and adapted backend code to it instead of demanding schema changes mid-project. That kept normalization clean.

**On Petar Marinović — Yes.**
Petar prototyped UI quickly and was honest when something would take longer than estimated. The dark theme decision was his and it ended up being one of the strongest visual elements.

**On Luka Horvat — Yes.**
Luka asked for the database dictionary draft a week before the deadline, not the night before, which is rare. He also independently verified that ON DELETE policies actually behaved correctly in both engines.

---

## Reviewer: Luka Horvat

**On Mateo Josipović — Yes.**
Mateo unblocked QA by handing me the audit log endpoint early so I could verify denied admin attempts were really being persisted.

**On Petar Marinović — Yes.**
Petar fixed every UI bug I filed within the same day and added regression-prone areas (admin forms, mobile width) to his own self-test list afterward.

**On Ana Kovačić — Yes.**
Ana gave me an ERD I could actually trace through during the manual test pass; I never had to ask "where does this column live?"

---

## Summary

| Pair | Outcome |
| --- | --- |
| Mateo ↔ Petar | Both Yes |
| Mateo ↔ Ana | Both Yes |
| Mateo ↔ Luka | Both Yes |
| Petar ↔ Ana | Both Yes |
| Petar ↔ Luka | Both Yes |
| Ana ↔ Luka | Both Yes |

All pairwise reviews are positive. There were no formal escalations to the project coordinator during the semester.
