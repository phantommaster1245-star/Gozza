# Source provenance

Gozza combines the owner's Full Lock Android source with PHANToM GPS / GPS Setter-derived source. The original package namespaces and contributor attribution in the GPS About screen are retained. GPS source is provided under GNU GPL v3; see LICENSE and gps/LICENSE. Third-party dependencies retain their own licenses. Gozza artwork is supplied by the project owner.

No private signing files, account credentials, backend secrets, device logs, or local investigation artifacts are included in this repository.

The API 102 GPS hook and Remote Preferences transport in `gps/src/modern` are adapted from the GeoX 0.1.07 published derivative patch at PHANToM-GPS commit `e98e077` (GPL v3). Gozza uses its own package, signing identity and tester-access service.
