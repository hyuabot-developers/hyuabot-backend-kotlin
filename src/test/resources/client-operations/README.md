These 54 GraphQL operation definitions are frozen copies of the current Android app, Android watch, and iOS client `.graphql` files. `manifest.txt` lists every source snapshot that the backend compatibility test validates against the current schema.

The snapshots keep backend CI independent of sibling client repositories. When a client operation changes, update the corresponding snapshot and manifest entry deliberately so the compatibility contract remains reviewable.
