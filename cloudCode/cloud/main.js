var TRANSACTION_NAME = "Transaction";
var SenderPublic = "SenderPublic";
var ReceiverPublic = "ReceiverPublic";
var ItemType = "ItemType";
var Amount = "Amount";
var EncryptedMessage = "EncryptedMessage"; 	// encrypted message
var HashArray = "HashArray"; 				// the important aspects of message that will be hashed
var Signature = "Signature"; 				// hash of hash_string that verifies sender actually sent with private key

var ASSET_NAME = "Asset";
var OwnerPublic = "OwnerPublic";  			// owner of the Asset
var ObjectId = "objectId";

// error codes
var ERROR_USER_LOOKUP_FAILED = "User lookup failed";
var ERROR_INSUFFICIENT_FUNDS = "Insufficient funds";
var ERROR_NOT_UNIQUE_TRANSACTION = "Not a unique transaction";
var ERROR_CANT_TRANSFER_NEGATIVE_VALUES = "Cannot transfer negative values";
var ERROR_NOT_VERIFIED_TRANSACTION = "Transaction was not verified correctly by sender";
var ERROR_UNKOWN_N_ONCE_ERROR = "Unknown NOnce check error";
var ERROR_DOES_NOT_OWN_ASSET = "Does not own asset"
var ERROR_UNKOWN_CHECK_OWNER = "Unknown check owner error";
var ERROR_UNKNOWN_ASSET = "Unknown asset";
var ERROR_GENERIC = "Generic error";

/**
 * get the total value of a given users account
 *  requirs as input:
 * User: the public key
 * ItemType: The type of item to query on
 * will return "User lookup failed on error
 * will return the total amount of 'itemType' on success
 * @param {} request the input
 * @param {} response the response
 * @returns {} get the total value of a given users account
 */
Parse.Cloud.define("userValue", function(request, response) {
  	// transaction the user has sent
  	var query = new Parse.Query(TRANSACTION_NAME);
  	query.equalTo(SenderPublic, request.params.User);
  	query.equalTo(ItemType, request.params.ItemType)
  	query.find({
    	success: function(results) {
      	var sum = 0;
      	for (var i = 0; i < results.length; ++i) {
        	sum -= results[i].get(Amount);
      	}
      
      	// transaction the user has receiver
  		var query2 = new Parse.Query(TRANSACTION_NAME);
  		query2.equalTo(ReceiverPublic, request.params.User);
  		query2.equalTo(ItemType, request.params.ItemType)
  		query2.find({
    		success: function(results) {
      			for (var i = 0; i < results.length; ++i) {
        			sum += results[i].get(Amount);
     			}
     			response.success(sum);
    		},
    		
    		error: function() {
      			response.error(ERROR_USER_LOOKUP_FAILED);
    		}
 		});
    },
    error: function() {
      response.error(ERROR_USER_LOOKUP_FAILED);
    }
  });
});

/**
 * Before we can save a transaction, make sure the user has enough items to transfer. and amount must be positive
 * will return "Insufficient funds" or "Cannot transfer negative values" on error
 * @param {} request request.object holds the transaction
 * @param {} response The response to send
 * @returns {} No return
 */
Parse.Cloud.beforeSave(TRANSACTION_NAME, function(request, response) {
	if (request.object.get(Amount) < 0){
		response.error(ERROR_CANT_TRANSFER_NEGATIVE_VALUES);
	}else{
		// get the total value of user
		Parse.Cloud.run('userValue', { User: request.object.get(SenderPublic) , ItemType: request.object.get(ItemType)}, {
  		success: function(totalValue) {
  			// see if asset even exists
  			doesAssetExist(request.object, {
  				success: function(){
  					// see if sender can transfer asset (either he owns it or has a balance sufficient to xfer)
  					canSenderTransferAmount(request.object, totalValue, {
    					success: function(canTransfer){
    						if (canTransfer){
    							// check this is a valid nOnce
    							isValidNOnce(request.object, {
    								success: function(){
    									if (isVerifiedBySender(request.object)){
    										response.success();
    									}else{
    										response.error(ERROR_NOT_VERIFIED_TRANSACTION);
    									}
    								},
    								error: function(error){
    									response.error(error);
    								}
    							});
    						}else{
    							response.error(ERROR_INSUFFICIENT_FUNDS);
    						}
    					},
    					error: function(error){
    						response.error(error);
    					}
    				});
    			},
    			error: function (error){
    				response.error(error);
    			}
    		});
  		},
  		error: function(error) {
  			response.error(error);
  		}
  		});
	}
});

/**
 * Check if the NOnce is valid
 * @param {} transaction
 * @param {} response. Call back with success or error. Error will have a string. success will be empty
 * @returns {} hting
 */
function isValidNOnce(transaction, response){
	//  perform query on transaction looking for a previous equal transaction hash array
	var Transaction = Parse.Object.extend(TRANSACTION_NAME);
	var query = new Parse.Query(Transaction);
	query.equalTo(HashArray, transaction.get(HashArray));
	
	// do the actual query
	query.first({
  		success: function(object) {
  		
  			// if the object is empty, then we are good, otherwise return an error
  			if (undefined == object){
  				response.success();
  			}else{
  				response.error(ERROR_NOT_UNIQUE_TRANSACTION);
  			}
  		},
  		error: function(error) {
  			// some unknown error
  			response.error(ERROR_UNKOWN_N_ONCE_ERROR);
  		}
	});
}

/**
 * Check if the transaction was actually sent by sender. Now just alwasy returns true
 * @param {} transaction
 * @returns {} 
 */
function isVerifiedBySender(transaction){
	return true;
}

/**
 * Check if the sender has at least enough of the asset, or if he is the sender
 * @param {} transaction
 * @param {} response success(true) if he does, success(false) otherwise. Error on unkown error
 * @returns {}  nothing
 */
function canSenderTransferAmount(transaction, sendersAccountValue, response){

	// first just check if we are positive, if so, then just say success
	if (transaction.get(Amount) <= sendersAccountValue){
		response.success(true);
		return;
	}
	
	// create the query matching object id and owner
	var Asset = Parse.Object.extend(ASSET_NAME);
	var query = new Parse.Query(Asset);
	query.equalTo(OwnerPublic, transaction.get(SenderPublic));
	query.equalTo(ObjectId, transaction.get(ItemType));
	
	// do the actual query
	query.first({
  		success: function(object) {
  		
  			// if the object is empty, then it does not match
  			if (undefined == object){
  				response.success(false);
  			}else{
  				response.success(true);
  			}
  		},
  		error: function(error) {
  			// some unknown error
  			response.error(ERROR_UNKOWN_CHECK_OWNER);
  		}
	});
}

/**
 * Check if the asset in the transaction exists
 * @param {} transaction The transaction to look at
 * @param {} response The response function, either success, or an error string
 * @returns {} 
 */
function doesAssetExist(transaction, response){
	// create the query matching object id of transaction of list of Assets
	var Asset = Parse.Object.extend(ASSET_NAME);
	var query = new Parse.Query(Asset);
	query.equalTo(ObjectId, transaction.get(ItemType));
	
	// do the actual query
	query.first({
  		success: function(object) {
  		
  			// if the object is empty, then there is none
  			if (undefined == object){
  				response.error(ERROR_UNKNOWN_ASSET);
  			}else if (object.length == 0){
  				response.error(ERROR_UNKNOWN_ASSET);
  			}else{
  				response.success();
  			}
  		},
  		error: function(error) {
  			// some unknown error
  			response.error(ERROR_GENERIC);
  		}
	});
}



