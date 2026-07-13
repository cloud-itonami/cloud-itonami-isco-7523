# cloud-itonami-isco-7523

Open Occupation Blueprint for **ISCO-08 7523**: Woodworking Machine Tool Setters and Operators.

**Maturity: `:implemented`** — WoodworkingAdvisor ⊣
WoodworkingMachineGovernor as a langgraph StateGraph
(`intake → advise → govern → decide → commit/hold`, human-approval
interrupt), modeled on cloud-itonami-isco-4311's bookkeeping actor.
14 tests / 30 assertions green. The governor never dispatches
hardware — it only gates what the material-feed robot below may
execute.

The cut-run HARD invariants — interval containment and arithmetic,
not a suggestion:

1. **Dimensional tolerance** — a proposed cut's measured dimension
   must fall inside the registered [target − tolerance, target +
   tolerance] band.
2. **Production ceiling** — a proposed run's units produced must not
   exceed the registered ordered quantity.

`:approve-unguarded-blade-operation` and
`:clear-quality-inspection-failure` **always** escalate to human
sign-off regardless of confidence, per this repo's Trust Controls
(business-model.md).

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
