# App library

## Optional selection mode

Set `"app_library_selection": true` along with `"app_library": true` in a theme,
or enable both booleans in `config.xml`, to use the library as a Home visibility
picker. It is off by default and the standard behavior below is unchanged.

In selection mode, OK toggles the green checkmark. Checked apps come first; both
groups are alphabetized by displayed name using the TV's locale. After hiding an
app, focus stays nearby on the next app (the previous app at the end), rather
than following the hidden app into the second group. Adding an app focuses it
in the checked group. Hold-OK actions are omitted in this mode; it only controls
Home shortcuts and never uninstalls apps.

## Standard behavior

This fork enables both `app_library` and `library_ok_opens` in `config.xml`.
Set `"app_library": false` in a theme to hide the library, or set
`"library_ok_opens": false` to use the upstream behavior: OK adds a missing
app to Home and opens an app that's already there. Selection mode takes
precedence over either OK behavior.

The fullscreen, five-column library lists installed apps with a TV or ordinary Android launch
activity, preferring the TV entry when both exist. It excludes services and apps
without a launch activity. Ordinary Android apps may still need touch or a mouse.

Tiles have no separate titles underneath. App names remain available to screen
readers and the filter. Apps without banners retain their icon and in-tile label.

Apps missing from Home come first. Apps first installed within the last seven
days lead that group, newest first; older missing apps are alphabetical. Apps
already on Home follow alphabetically, with a small Home badge. Updating an
existing app does not mark it as new.

- Press OK on any app to open it without changing its Home membership.
- Hold OK for Open, Add to Home or Remove from Home, and App info. The menu uses
  the shared glass action resources and stays anchored to the selected card.
- Back closes the menu first, then the library. Down from the last Home row or
  Right from the final app reaches the library pill; Up returns to the last row.
  Hebrew mirrors the horizontal direction, so Left from the final app reaches it.

With `"library_button": true` the library also has a round button in the top bar. Add
`"show_library_pill": false` to drop the pill after the last row; the button is then the way in, and
Down from the last row and Right from the final app (Left in Hebrew) stay where they are.

With `tile_menu` on, holding OK on a Home tile offers Remove from Home as well, next to Move and App info.
The remote's Menu key opens that same menu on the focused tile. With `tile_menu` off,
Menu keeps its normal behavior. Settings-key mappings take priority; Menu is left alone
on header buttons, while another popup is open, and during reordering.
Every app already on Home can be removed, including the theme’s default apps.
It stays installed and remains in the library, where Hold OK → Add to Home adds it back.
There is no hidden-app section and no uninstall action. Use
`library_excluded_apps` for packages that should never be offered, such as
factory-test tools. This is independent of `hidden_apps`, which hides only
the Home shortcut.

Choices are stored in the launcher’s private `library_apps.txt` and
`library_home_removed.txt`, with at most 128 entries in each. Each file is written
atomically. An unreadable file is left unchanged if an edit fails. Choices
survive restarts and app updates; missing or disabled packages are omitted until
they return. Explicit Home removals override theme defaults until re-added.
Theme config, dock ordering, module-card decisions and other saved tile order
are not edited. Disabling the library button keeps existing Home choices.

The library refreshes after package changes while visible and when resumed.
The catalogue loads off the UI thread; icons and banners load only for bound
cards. The highlight follows the launcher's animation and reduced motion settings. There is no polling, network, backdrop capture, new service,
permission or dependency. The additional manifest query covers ordinary
LAUNCHER activities on Android 11 and later.


Fork design history: [full-screen library design](superpowers/specs/2026-10-20-fullscreen-app-library-design.md)
and [implementation plan](superpowers/plans/2026-10-20-fullscreen-app-library.md).
