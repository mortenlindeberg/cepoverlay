/* -*- Mode:C++; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation;
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <vector>
#include <sstream>

#include "ns3/core-module.h"
#include "ns3/network-module.h"
#include "ns3/csma-module.h"
#include "ns3/tap-bridge-module.h"
#include "ns3/internet-module.h"
#include "ns3/applications-module.h"
#include "ns3/internet-apps-module.h"
#include "ns3/ipv4-static-routing-helper.h"


using namespace ns3;

NS_LOG_COMPONENT_DEFINE ("TapCsmaVirtualMachineExample");

/*
static void PingRtt (std::string context, Time rtt)
{
  std::cout << context << " " << rtt << std::endl;
}

static void IpRxTx(std::string context, Ptr< const Packet > packet, Ptr< Ipv4 > ipv4, uint32_t interface ) {
  Ipv4Header header;
  packet->PeekHeader(header);
  std::cout << Simulator::Now () << " " << context << " " << interface << " " << header.GetSource () << " -> " << header.GetDestination () << std::endl;
}
*/

int getIndex(std::string name, std::vector<std::string> nodeNames)
{
    for (uint32_t i = 0; i < nodeNames.size(); i++) {
        if (name.compare(nodeNames.at(i)) == 0) {
          return i;
        }
    }
    std::cout << name << " not in vector ";
    return -1;
}

std::vector<std::string> GetNodeNames(std::string nodeString)
{
  std::stringstream ssNodes (nodeString);
  std::vector<std::string> nodeNames;

  while (ssNodes.good ())
  {
    std::string nodestr;
    getline (ssNodes, nodestr, ',');
    std::stringstream ssNodestr (nodestr);
    std::string name;
    getline (ssNodestr, name, ':');
    nodeNames.push_back (name);
  }
  return nodeNames;
}

std::vector<std::string> GetNodeRates(std::string nodeString)
{
  std::stringstream ssNodes (nodeString);
  std::vector<std::string> nodeRates;

  while (ssNodes.good ())
  {
    std::string nodestr;
    getline (ssNodes, nodestr, ',');
    std::stringstream ssNodestr (nodestr);
    std::string name, address, port, cpu, rate;

    //D1:localhost:2005:1:1Mbps
    getline (ssNodestr, name, ':');
    getline (ssNodestr, address, ':');
    getline (ssNodestr, port, ':');
    getline (ssNodestr, cpu, ':');
    getline (ssNodestr, rate, ':');
    nodeRates.push_back (rate);

  }
  return nodeRates;
}


std::vector<std::vector<int> > GetLinks(std::string linkString, std::vector<std::string> nodeNames)
{
  std::vector<std::vector<int>> links;
  std::stringstream ssLink(linkString);
  while (ssLink.good ())
  {
    std::string lstr;
    getline (ssLink, lstr, ',');
    std::stringstream ssLstr(lstr);
    std::string from;
    getline(ssLstr, from, '-');
    std::string to;
    getline(ssLstr, to);
    int fromIndex = getIndex(from, nodeNames);
    int toIndex = getIndex(to, nodeNames);
    std::vector<int> link;
    link.push_back(fromIndex);
    link.push_back(toIndex);
    links.push_back(link);
  }
  return links;
}

int main (int argc, char *argv[])
{
  CommandLine cmd;
  std::string nodeString = "P1:NA,C1:NA,D1:NA";
  cmd.AddValue("nodes", "Comma separated list of nodes", nodeString);

  std::string linkString = "P1-C1,C1-D1";
  cmd.AddValue("links", "Comma separated list of links (node name - node name)", linkString);

  cmd.Parse (argc, argv);

  LogComponentEnable ("ArpCache", LOG_LEVEL_ALL);
  LogComponentEnable ("Ipv4L3Protocol", LOG_LEVEL_ALL);

  /* Use real-time simulation mode and enable checksum to be able to interact with real world */
  GlobalValue::Bind ("SimulatorImplementationType", StringValue ("ns3::RealtimeSimulatorImpl"));
  GlobalValue::Bind ("ChecksumEnabled", BooleanValue (true));

  std::vector<std::string> nodeNames = GetNodeNames(nodeString);
  std::vector<std::string> nodeRates = GetNodeRates(nodeString);

  std::vector<std::vector<int> > nodeLinks = GetLinks(linkString, nodeNames);

  // Create the nodes
  NodeContainer nodes;
  nodes.Create (nodeNames.size ());

  // Create the bridges that will link the CSMA networks
  NodeContainer routers;

  // Data structure that holds the list of routers in each node's CSMA network, and the inverse structure of the CSMA a router belongs to
  std::vector<NodeContainer> nodeRouters(nodeNames.size (), NodeContainer()) ; // Holds a list of bridges that node n connects to
  std::vector<NodeContainer> routerNodes; // Holds a list of nodes that this router n connects to


  for (uint32_t j = 0; j < nodeLinks.size (); j++) { // Run through all the links and create one router per link to connect the nodes
    int from = nodeLinks.at (j).at (0);
    int to = nodeLinks.at (j).at (1);
    Ptr<Node> router = CreateObject<Node> (); // Create the router
    Names::Add ("R"+nodeNames.at(from)+nodeNames.at (to), router); // Assign this router with a name using the ns-3 Name class :)
    routers. Add (router); // Add it to container of routers
    routerNodes.push_back (NodeContainer(nodes. Get (from))); // Store the information that router connects to node
    nodeRouters.at(from).Add (NodeContainer(router)); // Store the information that current node connects to the newly created router
    //std::cout << " -> We needed a new router no. " << routers.GetN () - 1 << " at address " << router << std::endl;


    routerNodes.at (routers.GetN () - 1).Add (nodes.Get (to)); // Store the destination node among the nodes connected to the router
    //std::cout << "   -> Router  " << routers.GetN () - 1 << " added node " << nodes.Get (nodeLinks.at (j).at (1)) << std::endl;
    nodeRouters.at (to).Add (routers.Get (routers.GetN () - 1)); // Store the router among the destination nodes routers
    //std::cout << "   -> Node " << to << " added router " << routers.GetN ()-1 << " (connects node " << from << " to node " << to << ")" << std::endl;
  }

  InternetStackHelper stack;
  stack.Install (routers);
  stack.EnableAsciiIpv4All ("ip");

  std::vector<NetDeviceContainer> nodeDevices;
  CsmaHelper csma;


  /* Now create the CSMA networks for the nodes, and add the bridges */
  for (uint32_t i = 0; i < nodes.GetN (); i++)
  {
    csma.SetChannelAttribute ("DataRate", StringValue(nodeRates.at(i)));
    NetDeviceContainer devices = csma.Install (NodeContainer (nodes.Get (i), nodeRouters.at (i))); // This CSMA netwok should hold the node and the bridges of that node
    nodeDevices.push_back (devices);
  }

  Ipv4AddressHelper address;
  std::ostringstream ipStart;
  ipStart << "10.0.0." << nodes.GetN () + 1; // All nodes in same subnet..
  address.SetBase ("10.0.0.0", "255.255.252.0", ipStart.str ().c_str ());


  Ipv4InterfaceContainer routerInterfaces;
  for (uint32_t i = 0; i < routers.GetN (); i++) {
    for (uint32_t j = 0; j < routers.Get (i)->GetNDevices (); j++) {
      if (routers.Get (i)->GetDevice(j)->GetObject<LoopbackNetDevice> ()  == 0) {
        routerInterfaces.Add (address.Assign (routers.Get (i)->GetDevice(j)));
        //std::cout << " --> Assigning IP address to " << routers.Get (i)->GetDevice(j)->GetAddress () << std::endl;
      }
    }
  }

  /* Create the tap device for each node */
  TapBridgeHelper tapBridge;
  tapBridge.SetAttribute ("Mode", StringValue ("UseBridge"));

  std::string taptext ("tap-");

  /* For each node, attach the tap bridge */
  for (uint32_t i = 0; i < nodeNames.size (); i++) {
    tapBridge.SetAttribute ("DeviceName", StringValue (taptext + nodeNames.at (i)));
    tapBridge.Install (nodes.Get (i), nodes.Get (i)->GetDevice(0));
  }


  /* Create the routes to the nodes from the routers */
  Ipv4StaticRoutingHelper routingHelper;
  stack.SetRoutingHelper (routingHelper);
  Ptr<OutputStreamWrapper> routingStream = Create<OutputStreamWrapper> ("routes.tr", std::ios::out);
  routingHelper.PrintRoutingTableAllAt (Seconds (2.), routingStream);

  for (uint32_t i = 0; i < routers.GetN (); i++) { /* Set up the routes for each router */
    std::string name =  Names::FindName(routers.Get (i));
    std::string routerFile = "../routes/"+name+"-ns3-routes.txt";
    //std::cout << "  -> Read in the routes registered for router " << name << " from file " << routerFile << std::endl;

    /* Clean the routing table, everything except the first one which is for the loopback interface */
    Ptr<Ipv4StaticRouting> router = routingHelper.GetStaticRouting (routers.Get (i)->GetObject<Ipv4> ());
    for (uint j = 1; j < router->GetNRoutes (); j++)
      router->RemoveRoute (j);


    std::ifstream infile(routerFile);
    if (infile.is_open()) {
        std::string a,b;
        int c;
        while (infile >> a >> b >> c) {
          //std::cout << name << ": " << a << " " << b << " " << c << " " << routers.Get (i)->GetDevice (c)->GetAddress () << std::endl;
          router->AddHostRouteTo (Ipv4Address (a.c_str()), Ipv4Address (b.c_str()), c);
        }
    }
    else {
        std::cout << "  -> ERROR: could not open the router file: " << routerFile << std::endl;
    }
    infile.close();
  }



  /* Do some pinging so we can see where the error is
  V4PingHelper ping1 = V4PingHelper ("10.0.0.2");
  V4PingHelper ping2 = V4PingHelper ("10.0.0.3");

  ApplicationContainer apps1 = ping1.Install (routers.Get (0));
  ApplicationContainer apps2 = ping2.Install (routers.Get (1));
  apps1.Start (Seconds (2.0));
  apps1.Stop (Seconds (10.0));
  apps2.Start (Seconds (2.0));
  apps2.Stop (Seconds (10.0));

*/
  //Config::Connect ("/NodeList/*/ApplicationList/*/$ns3::V4Ping/Rtt", MakeCallback (&PingRtt));

  //Config::Connect ("/NodeList/*/$ns3::Ipv4L3Protocol/Tx", MakeCallback (&IpRxTx));
  //Config::Connect ("/NodeList/*/$ns3::Ipv4L3Protocol/Rx", MakeCallback (&IpRxTx));

  AsciiTraceHelper ascii;
  //csma.EnableAsciiAll (ascii.CreateFileStream ("csma.tr"));
  csma.EnablePcapAll ("trace");


  Simulator::Stop (Seconds (2400.));
  Simulator::Run ();
  Simulator::Destroy ();
}
