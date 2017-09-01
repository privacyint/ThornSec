#!/bin/bash
rm -R cd loopdir > /dev/null 2>&1
mkdir loopdir
mount -o loop debian-8.5.0-i386-netinst.iso loopdir > /dev/null 2>&1
mkdir cd
rsync -a -H --exclude=TRANS.TBL loopdir/ cd > /dev/null 2>&1
umount loopdir > /dev/null 2>&1
gunzip cd/install.386/initrd.gz > /dev/null 2>&1
echo "preseed.cfg" | cpio -o -H newc -A -F cd/install.386/initrd > /dev/null 2>&1
gzip cd/install.386/initrd > /dev/null 2>&1
sed -i "s/timeout 0/timeout 10/g" cd/isolinux/isolinux.cfg > /dev/null 2>&1
cd cd > /dev/null 2>&1
md5sum `find -follow -type f` 2> /dev/null > md5sum.txt
cd ..
genisoimage -o output.iso -r -J -no-emul-boot -boot-load-size 4 -boot-info-table -b isolinux/isolinux.bin -c isolinux/boot.cat ./cd