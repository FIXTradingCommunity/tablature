# Rules of Engagement

Order messages and their elements.

## Message NewOrderSingle type 'D'

The new order message type is used by institutions wishing to electronically submit securities and forex orders to a broker for execution.

**Workflow**: The receiver of `NewOrderSingle` responds with an ExecutionReport.

| Name           | Tag | Presence                |
|----------------|----:|-------------------------|
| ClOrdID        | 11  | required                |
| Account        |     |                         |
| Instrument     | c   |                         |
| Side           |     | required                |
| Price          |     |                         |
| StopPx         |     | required when OrdType=3 |
| OrderQty       |     | required                |
| OrdType        |     | required                |
| MyUserDefined1 |     |                         |
| MyUserDefined2 | 6235|                         |	
| Parties        | g   |                         |

## Fields

| Name           | Tag | Type         |  Values                  |
|----------------|----:|--------------|--------------------------|
| Side           | 54  | Sides        |	                         |
| OrdType        | 40  | char         | 1=Market 2=Limit         |
| MyUserDefined1 | 6234| UTCTimestamp |	                         |

## Component Instrument 

The `Instrument` component block contains all the fields commonly used to describe a security or instrument.

| Name             | Tag | Presence  | Values 
|------------------|----:|-----------|--------
| SecurityID       | 48  | required  |        
| SecurityIDSource | 22  | constant  | 8      

## Group Parties 

The Parties component block is used to identify and convey information on the
entities both central and peripheral to the financial transaction represented
by the FIX message containing the `Parties` Block. 

| Name           | Tag | Presence  | Values |
|----------------|----:|-----------|--------|
| NoParties      | 453 |           |        |
| PartyID        |     | required  |        |
| PartyIDSource  |     |           |        |
| PartyRole      |     | required  | 1=ExecutingFirm 2=BrokerOfCredit 

### Codeset Sides 

Side of an order

| Name     | Value | Documentation |
|----------|-------|---------------|
| Buy      | 1     |               |
| Sell     | 2     |               |
| Cross    | 8     | Cross (orders where counterparty is an exchange, valid for all messages *except* IOIs) 
| Opposite | C     | "Opposite" (for use with multileg instruments) 

## Message CrossOrderCancelReplaceRequest type t (53)

Used to modify a cross order previously submitted using the New Order - Cross message. See Order Cancel Replace Request for details concerning message usage.

| Name                       | Tag       | Presence                                                 |
|----------------------------|-----------|----------------------------------------------------------|
| StandardHeader             | component | required                                                 |
| OrderID                    | 37        | optional                                                 |
| OrderRequestID             | 2422      | optional                                                 |
| CrossID                    | 548       | required                                                 |
| OrigCrossID                | 551       | required                                                 |
| HostCrossID                | 961       | optional                                                 |
| CrossType                  | 549       | required                                                 |
| CrossPrioritization        | 550       | required                                                 |
| RootParties                | group     | optional                                                 |
| SideCrossOrdModGrp         | group     | required                                                 |
| Instrument                 | component | required                                                 |
| UndInstrmtGrp              | group     | optional                                                 |
| InstrmtLegGrp              | group     | optional                                                 |
| SettlType                  | 63        | optional                                                 |
| SettlDate                  | 64        | optional                                                 |
| HandlInst                  | 21        | optional                                                 |
| ExecInst                   | 18        | optional                                                 |
| MinQty                     | 110       | optional                                                 |
| MinQtyMethod               | 1822      | optional                                                 |
| MatchIncrement             | 1089      | optional                                                 |
| MaxPriceLevels             | 1090      | optional                                                 |
| DisplayInstruction         | component | optional                                                 |
| MaxFloor                   | 111       | optional                                                 |
| MarketSegmentID            | 1300      | optional                                                 |
| ExDestination              | 100       | optional                                                 |
| ExDestinationIDSource      | 1133      | optional                                                 |
| TrdgSesGrp                 | group     | optional                                                 |
| ProcessCode                | 81        | optional                                                 |
| PrevClosePx                | 140       | optional                                                 |
| LocateReqd                 | 114       | optional                                                 |
| TransactTime               | 60        | required                                                 |
| TransBkdTime               | 483       | optional                                                 |
| Stipulations               | group     | optional                                                 |
| OrdType                    | 40        | required                                                 |
| PriceType                  | 423       | optional                                                 |
| Price                      | 44        | optional                                                 |
| PriceProtectionScope       | 1092      | optional                                                 |
| StopPx                     | 99        | required when OrdType == ^Stop \|\| OrdType == ^StopLimit |
| TriggeringInstruction      | component | optional                                                 |
| SpreadOrBenchmarkCurveData | component | optional                                                 |
| YieldData                  | component | optional                                                 |
| Currency                   | 15        | optional                                                 |
| ComplianceID               | 376       | optional                                                 |
| IOIID                      | 23        | optional                                                 |
| QuoteID                    | 117       | optional                                                 |
| TimeInForce                | 59        | optional                                                 |
| EffectiveTime              | 168       | optional                                                 |
| ExpireDate                 | 432       | optional                                                 |
| ExpireTime                 | 126       | optional                                                 |
| GTBookingInst              | 427       | optional                                                 |
| ExposureDuration           | 1629      | optional                                                 |
| ExposureDurationUnit       | 1916      | optional                                                 |
| TradingCapacity            | 1815      | optional                                                 |
| MaxShow                    | 210       | optional                                                 |
| PegInstructions            | component | optional                                                 |
| DiscretionInstructions     | component | optional                                                 |
| TargetStrategy             | 847       | optional                                                 |
| StrategyParametersGrp      | group     | optional                                                 |
| TargetStrategyParameters   | 848       | optional                                                 |
| ParticipationRate          | 849       | optional                                                 |
| CancellationRights         | 480       | optional                                                 |
| MoneyLaunderingStatus      | 481       | optional                                                 |
| RegistID                   | 513       | optional                                                 |
| Designation                | 494       | optional                                                 |
| ThrottleInst               | 1685      | optional                                                 |
| StandardTrailer            | component | required                                                 |
