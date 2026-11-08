"""Copy a jar, lowering class-file major versions above 65 to 65 so a JDK 21 compiler can read the
signatures. The copies are for compiling only and are never run or shipped."""
import sys, zipfile, struct
src, dst = sys.argv[1], sys.argv[2]
zi = zipfile.ZipFile(src); zo = zipfile.ZipFile(dst, 'w', zipfile.ZIP_STORED)
for info in zi.infolist():
    data = zi.read(info.filename)
    if info.filename.endswith('.class') and data[:4] == b'\xca\xfe\xba\xbe' and struct.unpack('>H', data[6:8])[0] > 65:
        data = data[:6] + struct.pack('>H', 65) + data[8:]
    if info.filename.endswith('.jar') or info.filename.startswith('META-INF/jars/'): continue
    zo.writestr(info.filename, data)
zo.close()
