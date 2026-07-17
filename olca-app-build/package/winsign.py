from pathlib import Path

import pefile


def remove_signature(exec: Path):
    if not exec.exists():
        print(f"  Warning: {exec} not found; cannot remove signature.")
        return

    try:
        with open(exec, "rb") as f:
            data = f.read()

        pe = pefile.PE(data=data)
        sec_dir_idx = pefile.DIRECTORY_ENTRY["IMAGE_DIRECTORY_ENTRY_SECURITY"]
        if sec_dir_idx >= len(pe.OPTIONAL_HEADER.DATA_DIRECTORY):
            pe.close()
            return

        security_dir = pe.OPTIONAL_HEADER.DATA_DIRECTORY[sec_dir_idx]
        if security_dir.VirtualAddress == 0 or security_dir.Size == 0:
            pe.close()
            return

        print(f"  Removing digital signature from {exec.name}...")
        address = security_dir.VirtualAddress

        # clear the security directory
        security_dir.VirtualAddress = 0
        security_dir.Size = 0

        new_data = pe.write()
        pe.close()

        # truncate the signature bytes (slice up to address)
        if address < len(new_data):
            new_data = new_data[:address]

        with open(exec, "wb") as f:
            f.write(new_data)
            print("  Signature removed successfully.")

    except Exception as e:
        print(f"  Error removing digital signature from {exec}: {e}")
