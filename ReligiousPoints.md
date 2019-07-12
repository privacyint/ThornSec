Religious point 1
We try to avoid using outside services that generate metadata or keep data unencrypted at rest.  This means we like to run our own internal services.

Religious point 2
We presume people are mobile and we try to avoid a situation where a laptop leaving the office has an unsustainable amount of data on it, encrypted or not, under the control of the user or not.
This means we want to reduce the attack surface when people use their computers on other networks (VPN), and we want to reduce the data held on a device (SVN).

Religious point 3
We presume that our services, particularly outside-facing ones, will be compromised and we want to reduce the implications — to fail well.
This means we keep as little sensitive information on outside servers — with the exception of Network which is mostly compromising information, and our fundraising data is compromising information (PI002).

Religious point 4
We presume that services are fragile.
If there is a power outage or damage to the office (or seizure of devices) backups exist, rebooting the office to a new installation takes minutes and little expertise, and if a service falls it will not be restarted until it can be audited to work out why.

Religious point 5
We want sustainable maintenance, development and sustainable services and learning.
Security is also about patching and maintenance and this should be made as easy as possible, and when hard the lessons can be shared. If there are attacks against our system we want to be able to log them to see if we can learn from them and share with others (MISP).
We use open source as much as we can so that anyone anywhere can adapt our software for use, and our lessons are shared openly too (Github).

