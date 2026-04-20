# Sound effects

Drop uncompressed `.wav` (preferred) or `.mp3` files here with the exact filenames below. The
`SoundManager` will pick them up on startup; anything missing is silently skipped so the game
still runs without audio.

| File                     | Played when…                       |
| ------------------------ | ---------------------------------- |
| `kick.wav`               | the player shoots                  |
| `goal.wav`               | the shot counts as a goal          |
| `save.wav`               | the keeper saves the shot          |
| `miss.wav`               | the shot misses (wide/over/post/…) |
| `level_complete.wav`     | a level is won                     |
| `level_failed.wav`       | a level is lost                    |
| `campaign_complete.wav`  | the full 3-level campaign is won   |

Keep clips short (under ~2s) and normalised; `AudioClip` is optimised for small, overlapping SFX.
