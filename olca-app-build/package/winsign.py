from pathlib import Path

import pefile


def remove_signature(exe: Path):
    if not exe.exists():
        print(f"  Warning: {exe} not found; cannot remove signature.")
        return

    try:
        pe = pefile.PE(exe)
        sec_dir_idx = pefile.DIRECTORY_ENTRY["IMAGE_DIRECTORY_ENTRY_SECURITY"]
        if sec_dir_idx >= len(pe.OPTIONAL_HEADER.DATA_DIRECTORY):
            pe.close()
            return

        security_dir = pe.OPTIONAL_HEADER.DATA_DIRECTORY[sec_dir_idx]
        if security_dir.VirtualAddress == 0 or security_dir.Size == 0:
            pe.close()
            return

        print(f"  Removing digital signature from {exe.name}...")
        # clear the security directory
        sig_address = security_dir.VirtualAddress
        sig_size = security_dir.Size
        security_dir.VirtualAddress = 0
        security_dir.Size = 0
        new_data = pe.write()
        pe.close()

        # remove the signature bytes
        if sig_address < len(new_data):
            new_data = (
                new_data[:sig_address] + new_data[sig_address + sig_size :]
            )

        with open(exe, "wb") as f:
            f.write(new_data)
            print("  Signature removed successfully.")

    except Exception as e:
        print(f"  Error removing digital signature from {exe}: {e}")
