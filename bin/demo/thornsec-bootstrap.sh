#!/bin/bash

function setRouterVars {
	VM="Thornsec-Router"
	DISKSIZE="8192"
	RAM="128"
	ISO="router.iso"
	NIC1="nat"
	NICTYPE1="virtio"
	NIC2="intnet"
	NICTYPE2="82545EM"
	PORT="6789"
	MAC1="aaeeeeeeeeee"
	MAC2="eeeeeeeeeeee"
}

function setVMVars {
	hostname=""
	while [[ $hostname = "" ]]; do
		echo -n "Hostname: "
		read hostname
	done

	VM="Thornsec-$hostname"
	
	disk=""
	while [[ ! $disk =~ ^[0-9]+$ ]]; do
		echo -n "Disk Size (GB): "
		read disk
	done
	let "DISKSIZE=$disk*1024"

	RAM=""
	while [[ ! $RAM =~ ^[0-9]+$ ]]; do
		echo -n "RAM (MB): "
		read RAM
	done
	
	ISO="$hostname.iso"
	NIC1="intnet"
	NICTYPE1="82545EM"
	NIC2="none"
	NICTYPE2="none"
	PORT="none"
	
	MAC1=""
	while [[ ! $MAC1 =~ ^([0-9A-Fa-f]{2}){5}([0-9A-Fa-f]{2})$ ]]; do
		echo -n "MAC address (format - aabbccddeeff): "
		read MAC1
	done
	
	MAC2="auto"
}

function deleteVM {
	echo -e "\033[0;31m"
	echo "************** WARNING *************"
	echo "* THIS WILL BLINDLY DELETE YOUR VM *"
	echo "*  ~THIS ACTION CANNOT BE UNDONE~  *"
	echo "*      _YOU HAVE BEEN WARNED!_     *"
	echo "************** WARNING *************"
	echo -e "\033[0m"
	echo -n "Machine hostname to delete: "
	read hostname
	VM="Thornsec-$hostname"
	#Delete the VM if it already exists
	VBoxManage controlvm $VM poweroff & > /dev/null 2>&1
	sleep 1 #Force blocking
	VBoxManage unregistervm $VM --delete > /dev/null 2>&1
}

function createVM {
	#Build and attach the HDDs & boot ISO
	VBoxManage createhd --filename $VM.vdi --size $DISKSIZE > /dev/null 2>&1
	VBoxManage createvm --name $VM --ostype "Debian" --register > /dev/null 2>&1
	VBoxManage storagectl $VM --name "SATA Controller" --add sata --controller IntelAHCI
	VBoxManage storageattach $VM --storagectl "SATA Controller" --port 0 \
							 --device 0 --type hdd --medium $VM.vdi
	VBoxManage storagectl $VM --name "IDE Controller" --add ide
#	VBoxManage storageattach $VM --storagectl "IDE Controller" --port 0 \
							 --device 0 --type dvddrive --medium $ISO
	VBoxManage modifyvm $VM --ioapic on
	VBoxManage modifyvm $VM --boot1 dvd --boot2 disk --boot3 none --boot4 none

	#Set up RAM
	VBoxManage modifyvm $VM --memory $RAM --vram 16

	#Add NICs
	VBoxManage modifyvm $VM --nic1 $NIC1
	VBoxManage modifyvm $VM --natnet1 "172.16/16" 
	VBoxManage modifyvm $VM --nictype1 $NICTYPE1
	VBoxManage modifyvm $VM --macaddress1 $MAC1
	VBoxManage modifyvm $VM --nic2 $NIC2 > /dev/null 2>&1
	VBoxManage modifyvm $VM --nictype2 $NICTYPE2 > /dev/null 2>&1
	VBoxManage modifyvm $VM --macaddress2 $MAC2 > /dev/null 2>&1

	#Start the VM
#	VBoxManage startvm $VM
	
	#Add the port forwarding
	VBoxManage controlvm $VM natpf1 ssh,tcp,,$PORT,,22
}

while true; do
	echo "Choose an option:"
	echo "1) Delete a VM"
	echo "2) Build & boot a Router VM"
	echo "3) Build & boot another VM"
	echo "4) Quit"
	read -p "Select your option: " opt
	case $opt in
		1 ) deleteVM;;
		2 ) setRouterVars;createVM;;
		3 ) setVMVars;createVM;;
		4 ) exit;;
	esac
done
