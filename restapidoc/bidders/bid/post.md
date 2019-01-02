# Create/Modify Auction

If Bidder exists and has already joined the Auction, place a Bid on this Auction.

**URL** : `/bidders/$id/bid`

**Method** : `POST`

**Auth required** : NO

**Permissions required** : None

**Data example** 

```json
{
    "auctionId": "1",
    "price": 350
}
```

## Success Response

**Condition** : Bidder and Auction exist for their respective ids.

**Code** : `200 OK`

**Content example**

```json
{
    "description": "Bid successful for Bidder 1, Auction 2 and a price of 350.0.",
    "success": true
}
```

## Error Responses

**Condition** : If the field is missing or of incorrect type.

**Code** : `400 BAD REQUEST`

**Content example**

```
The request content was malformed:
Expected String as JsString, but got 12
```

### Or

**Condition** : No Bidder exists in the system for bidderId.

**Code** : `400 BAD REQUEST`

**Content example**

```json
{
    "description": "No action performed. Bidder 2 does not exists.",
    "success": false
}
```

### Or

**Condition** : Bidder has not joined the Auction yet.

**Code** : `400 BAD REQUEST`

**Content example**

```json
{
    "description": "Bidder 1 has yet to join auction 2.",
    "success": false
}
```

### Or

**Condition** : Auction start date has not been reached yet.

**Code** : `400 BAD REQUEST`

**Content example**

```json
{
    "description": "Bid failed : too early too Bid ! Bid starting at 2019-01-13 12:12:12.0.",
    "success": false
}
```

### Or

**Condition** : Bid price is not sufficient.

**Code** : `400 BAD REQUEST`

**Content example**

```json
{
    "description": "Bid failed : Insufficient price. Minimum is 354.0.",
    "success": false
}
```

### Or

**Condition** : It's too late to Bid.

**Code** : `400 BAD REQUEST`

**Content example**

```json
{
    "description": "Bid failed : too late too Bid ! Bid closed at 2019-01-02 18:07:00.0.",
    "success": false
}
```