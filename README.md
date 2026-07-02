# cloud-itonami-isco-7523

Open Occupation Blueprint for **ISCO-08 7523**: Woodworking Machine Tool Setters and Operators.

This repository designs a forkable OSS business for an independent woodworking machine tool setter/operator: a material-feed robot performs lumber loading and cut-piece removal under a governor-gated actor, so the operator keeps their own production and safety records instead of renting a closed shop-management SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a material-feed robot performs lumber loading and cut-piece removal near woodworking machinery under an actor that proposes
actions and an independent **Woodworking Machine Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
operating near an unguarded blade, or clearing a quality-inspection failure) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
production order + material spec + safety guard protocol
        |
        v
Woodworking Advisor -> Woodworking Machine Governor -> cut/inspect, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `7523`). Required capabilities:

- :robotics
- :telemetry
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
