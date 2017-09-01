#!/bin/bash

function boot {
	echo "Booting the build environment - this may take a few seconds."
	VBoxManage startvm "Debian Build Environment" --type headless  > /dev/null 2>&1
}

function kill {
	echo "Good night, build environment. We'll see you again soon."	
	VBoxManage controlvm "Debian Build Environment" poweroff & > /dev/null 2>&1
	sleep 1
}

function doTheThing {
	user=ed
	host=127.0.0.1
	port=5678
	
	echo -n "Machine Hostname: "
	read hostname
	echo -n "$hostname's Domain: "
	read domain
	
	ssh $user@$host -p $port \
		"rm ~/buildiso.sh > /dev/null 2>&1; \
		rm ~/preseed.cfg > /dev/null 2>&1; \
		rm ~/output.iso > /dev/null 2>&1;"
		
	scp -P $port \
		actualbuildiso.sh preseed.cfg \
		$user@$host:~/
	
	ssh -t $user@$host -p $port \
		"chmod +x ~/actualbuildiso.sh > /dev/null 2>&1; \
		sed -i -e 's/<<HOSTNAME>>/$hostname/g' ~/preseed.cfg > /dev/null 2>&1; \
		sed -i -e 's/<<DOMAIN>>/$domain/g' ~/preseed.cfg > /dev/null 2>&1; \
		sudo ~/actualbuildiso.sh;"
		
	scp -P $port $user@$host:~/output.iso ./$hostname.iso
}

while true; do
	echo "Choose an option:"
	echo "1) Build an ISO"
	echo "2) Launch Bootstrapper"
	echo "3) Quit"
	read -p "Select your option: " opt
	case $opt in
		1 ) boot;doTheThing;kill;;
		2 ) ./thornsec-bootstrap.sh;exit;;
		3 ) exit;;
	esac
done