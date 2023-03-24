# xpanse

Xpanse is an Open Source project allowing to easily implement native managed 
service on any cloud service provider.

Xpanse unleash your cloud services by removing vendor lock-in and lock out. 
It standardizes and exposes cloud service providers core services, meaning 
that your xpanse service is portable (multi-cloud) on any cloud topology and 
provider. It also avoids tight coupling of your service to other cloud service 
provider services.

## APIs (core services)

Xpanse interacts directly with the fundamental APIs used by the cloud service 
provider to create managed service:

* **identity** dealing with access, users, groups, roles, ...
* **computing** abstracts the manipulation of virtual machines
* **storage** abstracts the manipulation of storage volumes
* **vpc** abstracts the manipulation of network devices
* **billing** registers the business model in the cloud provider billing system
* ...

## Configuration Language

A managed service is described using Open Services Cloud Configuration Language 
(OCL).

OCL is a json descriptor of a managed service, describing the expected final 
state of your service, interacting with the fundamental APIs:

```yaml
# The version of the OCL
version: 2.0
# The category of the service.
category: container
# The Service provided by the ISV, the name will be shown on the console as a service.
name: K8S cluster
# The version of the service, if the end-user want to select the version when they want to deploy the service.
serviceVersion: v1.26.2
# For the users may have more than one service, the @namespace can be used to separate the clusters.
description: This is an ehanced K8S cluster services by ISV-A.
namespace: ISV-A
# Icon for the service.
icon: |
  data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACYAAAAmCAYAAACoPemuAAAACXBIWXMAAAsTAAALEwEAmpwYAAAEyUlEQVR4nO1XW2geVRA+52yrVrS2hGjFineleKNVSrHgXVD0RSWgVaoijVJaa0l2Zv+GslBERLw81Ae1XkB8ihaxlZhkZ3ZpUqOFasVLRVG0oGjVqFjx1ja/zNnN/tez+dNEC9KBJcnJmdlvbt/MKnVEDof4g+do5IcM8Hq1jk5Rh12Az9dILxmg/Qa5bB/gv+RMwP73gJAvtoCQD+SA6h/ggxq4V8Hggn8fEPBlGnmrQRpzAmoGEHmr8pNLpx2P58fXaOCkCIAG3qSRNrvv0JhGel0BLZ4inLL2/Ogmg/R2K5FRQIs9jG9t5a5GHlTIl08eEyTzDfKOltOFXDYB3W87cxI6GqlfrUnmtIxLIz07KVBoa2mFRtpwCHrrWwZmgL+org0D9GWB8T0a6FVVGmpXPp+ngbcYpG/dQGhf3d/bWkPVncyrK+o3pd400gu15zSgSvFS+V+DjY5ezwui6w3QO3Wp26zCZIZBHqpy/HfVuXPmhLg84FvqPPrDAIEKorYsxaMG6W57uWvgDI30sAZ6vlIG/JpNT4lOEtAmIBQCtpSxJpkjugbp1/rGmRCYBn7MkYJPVLjlWBWGJkt3SVg+67KteRkg7cpTFsSd9hCjE1TYe5QBHnHYfnBCYE5l5LLQQX6xFC81wJ9mwJ6uRIzeyO5LZO+sAOZlzu6UCVEoYXKMQf7TYWC31IcKonPz+2tHZmnkxw1QWBXxTTaC67adnN+zEeubbYC+d3Tm18XAJAqubvLjlRnwvQaop0Zv7cisinN9s6sbIovUcAb6EZd91cWnOXEZZN+p2NN/qod0c1Vd1IJrIl7AHeObh8JoYaHjAd/uNGQ7qnmof0w9pker2vxvGepWMRieq5H7LIUEUVsKytJFZR1CXq3C5DhnnSFvdEcM+DuH4mcZsGdkENuilrrJ9ainiqs25Aa7+SwDtEoAjzO8pZ/m73i3OaogObugaxJ7J0ypoiFlQNdl0TngYXxjUXoN0lsOytgvEW2iwMvrQvuk8JAQblNmLg21q266KC90TE5XQXRm/v+ugRNtFzdIWStIlng+XWWA7q2lI7664Xpt/XBZ+bRIVhPhGDvP/PgK5W8/3iB9VJWO0aYvD0NjgH4ar0/blenZbVZfVingFbY5aqIWP9AYMeB7nB2TRrDPOoC8seosJ1Y7VqpGS92G4tvxBPRh0Tu8ILq2McJhaOTlbkUas4o2avxBRgFXWt2OXs8AvW+A35Pf5UjupvVJbAd3XdqaOP6cckowPNcgfV5gYI/9POtO5mnkVypzk1ZVFfF9qaPJDA38ojiiSskFBunnArs71Oq+o1WhYHShQf6twMhu2SoqzkRttS+lH1L2z+0tNMjfFNjbK+StWpG0SN1hN8i/1MxMiaKQLSRLbLeOi0+LCngrpQhpqsmIc/3BrGthcIHxozs08FPyU+pNvqYMRHfJfibjS5qh0MFW1p0GsTVC3LxQqV8iVvjBa0dWMt8AbXcU+8vqkKU01G6Qvqrzcp899+OVE6RbnmVCvAKyDvQuu3ROSYL4kpo6Abu1LhdqaAHYUMqPNV/uozXNMxWRod0CiPKED/BBD+gGNZ1iixynCCwgVNMunTtnaqAn7J4v352Tez5O154mn3pH5P8u/wCL4t6vwR2T7QAAAABJRU5ErkJggg==
# Reserved for CSP, aws,azure,ali,huawei and ...
cloudServiceProvider:
  name: huawei
  regions:
    - name: cn-southwest-2
      area: Asia Pacific
    - name: cn-north-4
      area: North America
billing:
  # The business model(`flat`, `exponential`, ...)
  model: flat
  # The rental period (`daily`, `weekly`, `monthly`, `yearly`)
  period: monthly
  # The billing currency (`euro`, `usd`, ...)
  currency: euro
# The flavor of the service, the @category/@name/@version/@flavor can locate the specific service to be deployed.
flavors:
  - name: 1-master-with-3-woker-nodes-normal
    # The fixed price during the period (the price applied one shot whatever is the service use)
    fixedPrice: 40
    # Properties for the service, which can be used by the deployment.
    property:
      worker_nodes_count: 3
      flavor_id: s7.xlarge.4
  - name: 1-master-with-3-woker-nodes-performance
    # The fixed price during the period (the price applied one shot whatever is the service use)
    fixedPrice: 40
    # Properties for the service, which can be used by the deployment.
    property:
      worker_nodes_count: 3
      flavor_id: c7.xlarge.4
  - name: 1-master-with-5-woker-nodes-normal
    # The fixed price during the period (the price applied one shot whatever is the service use)
    fixedPrice: 40
    # Properties for the service, which can be used by the deployment.
    property:
      worker_nodes_count: 5
      flavor_id: s7.xlarge.4
  - name: 1-master-with-5-woker-nodes-performance
    # The fixed price during the period (the price applied one shot whatever is the service use)
    fixedPrice: 40
    # Properties for the service, which can be used by the deployment.
    property:
      worker_nodes_count: 5
      flavor_id: c7.xlarge.4
deployment:
  # kind, Supported values are terraform, pulumi, crossplane.
  kind: terraform
  # Context for deployment: the context including some kind of parameters for the deployment, such as fix,variable.
  # - env: The value of the fix parameters are defined by the ISV with the @value at the initial time.
  # - variable: The value of the variable parameters are defined by the user on the console.
  # The parameters will be used to generate the API of the managed service.
  context:
    - name: HW_REGION_NAME
      description: huawei cloud region name.
      kind: env
      type: string
      mandatory: true
      validator: minLength=6|maxLength=26
    - name: HW_ACCESS_KEY
      description: Huawei cloud access key.
      kind: env
      type: string
      mandatory: true
    - name: HW_SECRET_KEY
      description: Huawei cloud secret key.
      kind: env
      type: string
      mandatory: true
    - name: admin_passwd
      description: The admin password of all nodes in the K8S cluster. If the value is empty, will create a random password.
      kind: variable
      type: string
      mandatory: false
      validator: minLength=8|maxLength=16|pattern=^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,16}$
    - name: vpc_id
      description: The sub network id of all nodes in the K8S cluster. If the value is empty, will create a new VPC.
      kind: variable
      type: string
      mandatory: false
    - name: subnet_id
      description: The sub network id of all nodes in the K8S cluster. If the value is empty, will create a new subnet.
      kind: variable
      type: string
      mandatory: false
    - name: secgroup_id
      description: The security group id of all nodes in the K8S cluster. If the value is empty, will create a new security group.
      kind: variable
      type: string
      mandatory: false
  deployer: |
    data "huaweicloud_availability_zones" "osc-az" {}

    variable "admin_passwd" {
      type        = string
      default     = ""
      description = "The root password of all nodes in the K8S cluster."
    }

    variable "vpc_id" {
      type        = string
      default     = ""
      description = "The vpc id of all nodes in the K8S cluster."
    }

    variable "subnet_id" {
      type        = string
      default     = ""
      description = "The subnet id of all nodes in the K8S cluster."
    }

    variable "secgroup_id" {
      type        = string
      default     = ""
      description = "The security group id of all nodes in the K8S cluster."
    }

    data "huaweicloud_vpcs" "existing" {
      id = var.vpc_id
    }

    resource "random_id" "new" {
      byte_length = 8
    }

    resource "random_password" "password" {
      length  = 12
      special = true
      upper   = true
      lower   = true
      numeric = true
    }

    resource "huaweicloud_vpc" "new" {
      count = length(data.huaweicloud_vpcs.existing.vpcs) == 0 ? 1 : 0
      name  = "K8S-vpc-web-${random_id.new.hex}"
      cidr  = "192.168.0.0/16"
    }

    data "huaweicloud_vpc_subnets" "existing" {
      id = var.subnet_id
    }

    resource "huaweicloud_vpc_subnet" "new" {
      count      = length(data.huaweicloud_vpc_subnets.existing.subnets) == 0 ? 1 : 0
      vpc_id     = local.vpc_id
      name       = "K8S-subnet-${random_id.new.hex}"
      cidr       = "192.168.10.0/24"
      gateway_ip = "192.168.10.1"
    }

    data "huaweicloud_networking_secgroups" "existing" {
      secgroup_id = var.secgroup_id
    }

    resource "huaweicloud_networking_secgroup" "new" {
      count       = length(data.huaweicloud_networking_secgroups.existing.security_groups) == 0 ? 1 : 0
      name        = "K8S_secgroup-${random_id.new.hex}"
      description = "K8S cluster security group"
    }

    locals {
      admin_passwd = var.admin_passwd == "" ? random_password.password.result : var.admin_passwd
      vpc_id       = length(data.huaweicloud_vpcs.existing.vpcs) > 0 ? data.huaweicloud_vpcs.existing.vpcs[0].id : huaweicloud_vpcs.new[0].id
      subnet_id    = length(data.huaweicloud_vpc_subnets.existing.subnets) > 0 ? data.huaweicloud_vpc_subnets.existing.subnets[0].id : huaweicloud_vpc_subnet.new[0].id
      secgroup_id  = length(data.huaweicloud_networking_secgroups.existing.security_groups) > 0 ? data.huaweicloud_networking_secgroups.existing.security_groups[0].id : huaweicloud_networking_secgroup.new[0].id
    }

    data "huaweicloud_images_image" "k8s-image" {
      name        = "K8S-v1.26.2_Centos-7.9"
      most_recent = true
    }

    resource "huaweicloud_compute_instance" "k8s-master" {
      availability_zone  = data.huaweicloud_availability_zones.osc-az.names[0]
      name               = "k8s-master"
      flavor_id          = var.flavor_flavor_id
      security_group_ids = [ local.security_group_id ]
      image_id           = data.huaweicloud_images_image.k8s-image.id

      network {
        uuid = local.subnet_id
      }

      user_data = <<EOF
        #!bin/bash
        echo root:${local.admin_passwd} | sudo chpasswd
        sh /root/k8s-init.sh true ${local.admin_passwd} ${var.flavor_worker_nodes_count} > /root/init.log
      EOF
    }

    resource "huaweicloud_compute_instance" "k8s-node" {
      count              = var.flavor_worker_nodes_count
      availability_zone  = data.huaweicloud_availability_zones.osc-az.names[0]
      name               = "k8s-node-${count.index}"
      flavor_id          = var.flavor_flavor_id
      security_group_ids = [ local.security_group_id ]
      image_id           = data.huaweicloud_images_image.k8s-image.id

      network {
        uuid = local.subnet_id
      }

      user_data = <<EOF
        #!bin/bash
        echo root:${local.admin_passwd} | sudo chpasswd
        sh /root/k8s-init.sh false ${local.admin_passwd} ${var.flavor_worker_nodes_count} ${huaweicloud_compute_instance.k8s-master.access_ip_v4} > /root/init.log
      EOF

      depends_on = [
        huaweicloud_compute_instance.k8s-master
      ]
    }

    output "k8s_master_endpoint" {
      value = "${huaweicloud_compute_instance.k8s-master.access_ip_v4}:22"
    }

    output "k8s_master_host" {
      value = huaweicloud_compute_instance.k8s-master.access_ip_v4
    }

    output "master_admin_passwd" {
      value = nonsensitive(local.admin_passwd)
    }
```

## OCL loading

Xpanse provides different options to generate and provision OCL:

* REST API on the xpanse runtime
* CLI allowing to directly interact with xpanse via command line
* language frontend (SDL) for Java, Python, ...

## Orchestrator & binding

OCL descriptor is an abstract description of the final managed service state. 
It's generic enough to work with any cloud service provider.

Xpanse runtime embeds an orchestrator responsible to delegate the services 
management to plugins.

Each plugin is dedicated to handle a cloud provider infrastructure and do 
actions required to actually deal with the services' lifecycle:

1. to bind OCL to the concrete cloud provider internal APIs
2. to generate the graph of actions required to reach the final expected state, 
specifically for a target cloud provider

## Runtime

Xpanse runtime is the overall component running on the cloud provider.

The runtime embeds and run together:

1. the orchestrator with the different bindings
2. the OCL loader and parser
3. the frontends (REST API, ...)

## Database

The default database attached to the runtime is the H2 in-memory database. 
The same can be replaced with other production ready database systems by 
replacing the configurations mentioned below and by adding relevant maven 
dependencies.

```
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto= update
```

### Build and Package

First, you can build the whole xpanse project, including all modules 
(orchestrator, OCL, runtime, plugins, etc), simply with:

```shell
$ mvn clean install
```

### Run

By default, the application will not activate any plugins. They must be 
activated via spring profiles. Also ensure that only one plugin is active at a 
time.

* for Huawei Cloud:

```shell
$ cd runtime/target
$ java -jar xpanse-runtime-1.0.0-SNAPSHOT.jar -Dspring.profiles.active=huaweicloud
```

* for Openstack:

```shell
$ cd runtime/target
$ java -jar xpanse-runtime-1.0.0-SNAPSHOT.jar -Dspring.profiles.active=openstack
```

By default, the runtime is built in "exploded mode". Additionally, you can also 
build a Docker image adding `-Ddocker.skip=false` as build argument:

```shell
$ cd runtime
$ mvn clean install -Ddocker.skip=false
```

We can start xpanse runtime with a specific plugin by passing the plugin name 
in the profile name. For example to start huaweicloud

```shell
$ docker run -e "SPRING_PROFILES_ACTIVE=huaweicloud" --name my-xpanse-runtime xpanse
```

### Static Code Analysis using CheckStyle

This project using `CheckStyle` framework to perform static code analysis. The 
configuration can be found in [CheckStyle](checkstyle.xml). The framework also 
checks the code format in accordance to `Google Java Format`.

The same file can also be imported in IDE CheckStyle plugins to get the 
analysis results directly in IDE and also to perform code formatting directly 
in IDE.

The framework is added as a maven plugin and is executed by default as part of 
the `verify` phase. Any violations will result in build failure.

### License/Copyright Configuration

All files in the repository must contain a license header in the format 
mentioned in [License Header](license.header).

The static code analysis framework will also validate if the license exists in 
the specified format.
