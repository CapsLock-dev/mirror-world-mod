# MirrorWorld (WIP)

Adding dimension where player can experiment or plan future building without affecting their real world.

### Mod info
**Loader: Fabric**

**Version: 1.21.11**

### Mod adds:
- Mirror dimension, which is overworld with the same seed.
- `/mirror_tp`, `/mirror_exit` to teleport to/from mirror.
- `/mark_chunk`, `/unmark_chunk` to mark/unmark chunks.
- `/copy_chunk` to queue copying of all marked chunks.

### Usage
1. Stand in the chunk you want to mirror.
2. Run `/mark_chunk` (repeat for multiple chunks).
3. Run `/copy_chunk` to copy all marked chunks to mirror world.
4. Run `/mirror_tp` to enter your mirror world and `/mirror_exit` to return to overworld.

### Known issues
- Doesn't copy entities, block entities and don't update light.

### Will add in future
- Separate player data in mirror dimension (inventory, achievements, etc.)
- Creative mode in mirror dimension
- Admin commands in mirror dimension 