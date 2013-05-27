#import <Foundation/NSObject.h>

typedef void (*fun_t)(void *);

@interface A : NSObject
+ (void) invoke: (fun_t) callback;

+ (void) checkIfEqualsToThis: (void *) pointer;
@end
