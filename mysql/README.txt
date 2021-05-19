Run mysql as root user
Run the following to create a new MySQL user:

CREATE USER 'k8sdemo'@'%' IDENTIFIED BY 'k8sdemo';
GRANT ALL PRIVILEGES ON *.* TO 'k8sdemo'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;

Exit mysql and open mysql with new k8sdemo username and password:
mysql -u k8sdemo -p

Run:
create database k8sdemo;
use k8sdemo;

Run customer.sql script